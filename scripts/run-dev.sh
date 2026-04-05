#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

quarkus_version="3.32.2"
# In WSL, bind to all interfaces so Windows browsers can reach the app.
# Outside WSL, keep the safer loopback default unless explicitly overridden.
if [[ -n "${QUARKUS_HTTP_HOST:-}" ]]; then
  host="$QUARKUS_HTTP_HOST"
elif [[ -n "${WSL_DISTRO_NAME:-}" || -n "${WSL_INTEROP:-}" || -n "${WSLENV:-}" || -r /proc/sys/kernel/osrelease && "$(tr '[:upper:]' '[:lower:]' </proc/sys/kernel/osrelease)" == *microsoft* ]]; then
  host="0.0.0.0"
else
  host="127.0.0.1"
fi
port="8090"
runtime_pidfile="$repo_root/target/run-dev.pid"
runtime_pid=""

cleanup_runtime() {
  if [[ -n "${runtime_pid:-}" ]] && kill -0 "$runtime_pid" 2>/dev/null; then
    kill -9 "$runtime_pid" 2>/dev/null || true
  fi

  if [[ -f "$runtime_pidfile" ]]; then
    rm -f "$runtime_pidfile"
  fi
}

choose_dev_mvn() {
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
  return 1
}

choose_build_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    printf '%s\n' "mvn"
    return
  fi

  local bundled_mvn="/tmp/quarkus-maven/apache-maven-3.9.12/bin/mvn"
  if [[ -x "$bundled_mvn" ]]; then
    printf '%s\n' "$bundled_mvn"
    return
  fi

  echo "No Maven installation found." >&2
  exit 1
}

stop_listener_on_port() {
  local listen_port="$1"
  local pids=()

  if command -v ss >/dev/null 2>&1; then
    mapfile -t pids < <(ss -H -ltnp "sport = :${listen_port}" 2>/dev/null | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' | sort -u)
  elif command -v lsof >/dev/null 2>&1; then
    mapfile -t pids < <(lsof -t -iTCP:"${listen_port}" -sTCP:LISTEN 2>/dev/null | sort -u)
  fi

  if [[ "${#pids[@]}" -gt 0 ]]; then
    echo "Stopping existing process on port ${listen_port} to avoid a stale runtime: ${pids[*]}"
    kill -9 "${pids[@]}" 2>/dev/null || true
    sleep 1
  fi
}

stop_stale_runtime_from_pidfile() {
  if [[ ! -f "$runtime_pidfile" ]]; then
    return
  fi

  local stale_pid
  stale_pid="$(cat "$runtime_pidfile" 2>/dev/null || true)"
  if [[ -n "$stale_pid" ]] && kill -0 "$stale_pid" 2>/dev/null; then
    echo "Stopping previous dev runtime from $runtime_pidfile: $stale_pid"
    kill -9 "$stale_pid" 2>/dev/null || true
    sleep 1
  fi

  rm -f "$runtime_pidfile"
}

stop_stale_runtime_processes() {
  local pids=()

  if command -v ps >/dev/null 2>&1 && command -v rg >/dev/null 2>&1; then
    mapfile -t pids < <(
      ps -ef | rg "quarkus-run\.jar|quarkus-maven-plugin:${quarkus_version}:dev" | rg -v "rg " | awk '{print $2}' | sort -u
    )
  fi

  if [[ "${#pids[@]}" -gt 0 ]]; then
    echo "Stopping stale Quarkus runtime processes: ${pids[*]}"
    kill -9 "${pids[@]}" 2>/dev/null || true
    sleep 1
  fi
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

export QUARKUS_HTTP_PORT="$port"

if [[ -z "${GAIA_DATASET_URL:-}" ]]; then
  export GAIA_LOCAL_PATH="${GAIA_LOCAL_PATH:-$repo_root/target/gaia-data}"
  "$repo_root/scripts/setup-gaia-local.sh"
fi

trap cleanup_runtime EXIT INT TERM

stop_stale_runtime_from_pidfile
stop_stale_runtime_processes
stop_listener_on_port "$port"

if mvn_bin="$(choose_dev_mvn)"; then
  repo_local="${M2_REPO:-$HOME/.m2/repository}"

  "$mvn_bin" \
    -Dmaven.repo.local="$repo_local" \
    io.quarkus.platform:quarkus-maven-plugin:"$quarkus_version":dev \
    -Dquarkus.http.host="$host" \
    -Ddebug=false &
  runtime_pid="$!"
  printf '%s\n' "$runtime_pid" >"$runtime_pidfile"
  wait "$runtime_pid"
fi

echo "Falling back to packaged runtime build on port ${port}." >&2
build_mvn="$(choose_build_mvn)"
repo_local="${M2_REPO:-$HOME/.m2/repository}"
"$build_mvn" \
  -Dmaven.repo.local="$repo_local" \
  -DskipTests \
  package

java \
  -Dquarkus.http.host="$host" \
  -jar target/quarkus-app/quarkus-run.jar &
runtime_pid="$!"
printf '%s\n' "$runtime_pid" >"$runtime_pidfile"
wait "$runtime_pid"
