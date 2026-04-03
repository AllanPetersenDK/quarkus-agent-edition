#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

quarkus_version="3.32.2"
host="0.0.0.0"

choose_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    local version
    version="$(mvn -v 2>/dev/null | awk '/Apache Maven/ {print $3; exit}')"
    local major minor
    IFS=. read -r major minor _ <<<"${version:-0.0.0}"
    if [[ "${major:-0}" -gt 3 || ( "${major:-0}" -eq 3 && "${minor:-0}" -ge 9 ) ]]; then
      printf '%s\n' "mvn"
      return
    fi
  fi

  local bundled_mvn="/tmp/quarkus-maven/apache-maven-3.9.12/bin/mvn"
  if [[ -x "$bundled_mvn" ]]; then
    printf '%s\n' "$bundled_mvn"
    return
  fi

  echo "No Maven 3.9+ found." >&2
  echo "Install Apache Maven 3.9+ or place it at /tmp/quarkus-maven/apache-maven-3.9.12/bin/mvn." >&2
  exit 1
}

if [[ -z "${JAVA_HOME:-}" && -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -f "$repo_root/.env" ]]; then
  # Load local workspace secrets without committing them.
  set -a
  # shellcheck disable=SC1090,SC1091
  source "$repo_root/.env"
  set +a
fi

mvn_bin="$(choose_mvn)"
repo_local="${M2_REPO:-$HOME/.m2/repository}"

exec "$mvn_bin" \
  -Dmaven.repo.local="$repo_local" \
  io.quarkus.platform:quarkus-maven-plugin:"$quarkus_version":dev \
  -Dquarkus.http.host="$host" \
  -Ddebug=false
