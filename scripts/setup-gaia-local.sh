#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_file="$repo_root/.env"

if [[ -f "$source_file" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$source_file"
  set +a
fi

gaia_root="${GAIA_LOCAL_PATH:-$repo_root/target/gaia-data}"
gaia_config="${GAIA_DEFAULT_CONFIG:-2023}"
gaia_split="${GAIA_DEFAULT_SPLIT:-validation}"
gaia_tree_url="https://huggingface.co/api/datasets/gaia-benchmark/GAIA/tree/main/${gaia_config}/${gaia_split}?recursive=1"
gaia_base_url="https://huggingface.co/datasets/gaia-benchmark/GAIA/resolve/main"
metadata_file="$gaia_root/${gaia_config}/${gaia_split}/metadata.level1.parquet"

if [[ -f "$metadata_file" && -s "$metadata_file" ]]; then
  echo "GAIA local snapshot already present at $gaia_root"
  exit 0
fi

token="${GAIA_HF_TOKEN:-${HF_TOKEN:-${HUGGINGFACE_TOKEN:-}}}"
if [[ -z "${token:-}" ]]; then
  echo "GAIA local setup requires HF_TOKEN, GAIA_HF_TOKEN, or HUGGINGFACE_TOKEN in the environment." >&2
  echo "The workspace-local snapshot is not present yet: $metadata_file" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "GAIA local setup requires jq for parsing the Hugging Face file list." >&2
  exit 1
fi

mkdir -p "$gaia_root"

echo "Downloading GAIA validation snapshot into $gaia_root"

file_list="$(
  curl -fsSL \
    -H "Authorization: Bearer $token" \
    "$gaia_tree_url" \
  | jq -r '.[] | select(.type == "file") | .path'
)"

if [[ -z "${file_list// }" ]]; then
  echo "GAIA local setup could not find any validation files from $gaia_tree_url" >&2
  exit 1
fi

while IFS= read -r relative_path; do
  [[ -z "$relative_path" ]] && continue
  local_file="$gaia_root/$relative_path"
  mkdir -p "$(dirname "$local_file")"
  if [[ -s "$local_file" ]]; then
    continue
  fi
  echo "Downloading $relative_path"
  curl -fsSL \
    -H "Authorization: Bearer $token" \
    "$gaia_base_url/$relative_path" \
    -o "$local_file"
done <<< "$file_list"

if [[ ! -f "$metadata_file" || ! -s "$metadata_file" ]]; then
  echo "GAIA local setup finished, but the expected metadata file is still missing: $metadata_file" >&2
  exit 1
fi

echo "GAIA local snapshot ready: $gaia_root"
