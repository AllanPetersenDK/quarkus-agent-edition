#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <chapter-number>" >&2
  exit 1
fi

chapter="$1"
case "$chapter" in
  02|2) test_name="Chapter02SmokeTest" ;;
  03|3) test_name="Chapter03SmokeTest" ;;
  04|4) test_name="Chapter04SmokeTest" ;;
  05|5) test_name="Chapter05SmokeTest" ;;
  06|6) test_name="Chapter06SmokeTest" ;;
  07|7) test_name="Chapter07SmokeTest" ;;
  08|8) test_name="Chapter08SmokeTest" ;;
  09|9) test_name="Chapter09SmokeTest" ;;
  10) test_name="Chapter10SmokeTest" ;;
  *)
    echo "Unknown chapter: $chapter" >&2
    exit 1
    ;;
esac

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if [[ -z "${JAVA_HOME:-}" && -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if command -v mvn >/dev/null 2>&1; then
  mvn_version="$(mvn -v 2>/dev/null | awk '/Apache Maven/ {print $3; exit}')"
  IFS=. read -r major minor _ <<<"${mvn_version:-0.0.0}"
  if [[ "${major:-0}" -ge 3 && "${minor:-0}" -ge 9 || "${major:-0}" -gt 3 ]]; then
    mvn_bin="mvn"
  else
    mvn_bin="/tmp/quarkus-maven/apache-maven-3.9.12/bin/mvn"
  fi
else
  mvn_bin="/tmp/quarkus-maven/apache-maven-3.9.12/bin/mvn"
fi

if [[ ! -x "$mvn_bin" ]]; then
  echo "No Maven 3.9+ found. Install Maven 3.9+ or run scripts/run-dev.sh first to use the bundled fallback." >&2
  exit 1
fi

repo_local="${M2_REPO:-$HOME/.m2/repository}"
exec "$mvn_bin" -Dmaven.repo.local="$repo_local" -Dtest="$test_name" test
