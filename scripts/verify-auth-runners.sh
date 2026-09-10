#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <maven-package-log>" >&2
  exit 2
fi

package_log=$1
repo_root=$(cd "$(dirname "$0")/.." && pwd)
[[ -f "$package_log" ]] || { echo "missing package log: $package_log" >&2; exit 1; }

duplicate_files=$(awk '
  /contain duplicate files:/ { collecting=1; next }
  collecting && /^- / { sub(/^- /, ""); print; next }
  collecting { collecting=0 }
' "$package_log")

while IFS= read -r duplicate; do
  [[ -z "$duplicate" ]] && continue
  case "$duplicate" in
    META-INF/versions/9/OSGI-INF/MANIFEST.MF|\
    META-INF/versions/11/OSGI-INF/MANIFEST.MF|\
    META-INF/Schubfach-LICENSE|\
    META-INF/FastDoubleParser-LICENSE|\
    META-INF/FastDoubleParser-ThirdParty-LICENSE) ;;
    *) echo "unexpected duplicate uber-jar entry: $duplicate" >&2; exit 1 ;;
  esac
done <<< "$duplicate_files"

current_pid=
current_log=
current_status=
cleanup() {
  if [[ -n "$current_pid" ]] && kill -0 "$current_pid" 2>/dev/null; then
    kill -TERM "$current_pid" 2>/dev/null || true
    for _ in $(seq 1 50); do
      kill -0 "$current_pid" 2>/dev/null || break
      sleep 0.1
    done
    if kill -0 "$current_pid" 2>/dev/null; then
      kill -KILL "$current_pid" 2>/dev/null || true
    fi
    wait "$current_pid" 2>/dev/null || true
  fi
  [[ -z "$current_log" ]] || rm -f "$current_log"
  [[ -z "$current_status" ]] || rm -f "$current_status"
  current_pid=
  current_log=
  current_status=
}
trap cleanup EXIT
trap 'cleanup; exit 130' INT
trap 'cleanup; exit 143' TERM

request() {
  curl --connect-timeout 2 --max-time 5 --fail --silent --show-error "$@"
}

smoke() {
  local auth=$1 header=$2 budget=$3
  local module="$repo_root/ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-$auth"
  local jar log status pid ready=false alice bob size
  jar=$(find "$module/target" -maxdepth 1 -name '*-runner.jar' -print -quit)
  [[ -n "$jar" ]] || { echo "missing runner jar for $auth" >&2; exit 1; }
  size=$(wc -c < "$jar" | tr -d ' ')
  (( size <= budget )) || { echo "$auth runner size $size exceeds budget $budget" >&2; exit 1; }
  log=$(mktemp "/tmp/ddd4j-quarkus-auth-$auth.XXXXXX.log")
  status=$(mktemp "/tmp/ddd4j-quarkus-auth-$auth.XXXXXX.status.json")
  current_log=$log
  current_status=$status
  java -Dquarkus.http.host=127.0.0.1 -Dquarkus.http.port=0 -jar "$jar" >"$log" 2>&1 &
  pid=$!
  current_pid=$pid
  local port=
  for _ in $(seq 1 120); do
    kill -0 "$pid" 2>/dev/null || { tail -100 "$log" >&2; return 1; }
    port=$(sed -nE 's/.*Listening on: http:\/\/127\.0\.0\.1:([0-9]+).*/\1/p' "$log" | tail -1)
    if [[ -n "$port" ]] && request "http://127.0.0.1:$port/auth/status" >"$status" 2>/dev/null; then ready=true; break; fi
    sleep 0.25
  done
  $ready || { tail -100 "$log" >&2; return 1; }
  jq -e '.login == false' "$status" >/dev/null
  alice=$(request -H 'Content-Type: text/plain' --data alice "http://127.0.0.1:$port/auth/login" | jq -er '.token')
  bob=$(request -H 'Content-Type: text/plain' --data bob "http://127.0.0.1:$port/auth/login" | jq -er '.token')
  request -H "$header: $alice" "http://127.0.0.1:$port/auth/status" | jq -e '.login == true' >/dev/null
  request -H "$header: $alice" "http://127.0.0.1:$port/auth/me" | jq -e '.authenticated == true and .loginId == "alice"' >/dev/null
  request -H "$header: $bob" "http://127.0.0.1:$port/auth/me" | jq -e '.authenticated == true and .loginId == "bob"' >/dev/null
  request "http://127.0.0.1:$port/auth/me" | jq -e '.authenticated == false' >/dev/null
  request -H "$header: $alice" "http://127.0.0.1:$port/auth/check/role?role=user" | jq -e '.has == true' >/dev/null
  request -H "$header: $alice" "http://127.0.0.1:$port/auth/check/role?role=admin" | jq -e '.has == false' >/dev/null
  request -H "$header: $alice" "http://127.0.0.1:$port/auth/check/permission?permission=profile%3Aread" | jq -e '.has == true' >/dev/null
  request -H "$header: $alice" "http://127.0.0.1:$port/auth/check/permission?permission=admin%3Awrite" | jq -e '.has == false' >/dev/null
  request -X POST -H "$header: $alice" "http://127.0.0.1:$port/auth/logout" | jq -e '.success == true' >/dev/null
  request -H "$header: $alice" "http://127.0.0.1:$port/auth/status" | jq -e '.login == false' >/dev/null
  request -H "$header: $bob" "http://127.0.0.1:$port/auth/status" | jq -e '.login == true' >/dev/null
  request -X POST -H "$header: $bob" "http://127.0.0.1:$port/auth/logout" | jq -e '.success == true' >/dev/null
  cleanup
  echo "auth runner verified: $auth ($size/$budget bytes)"
}

smoke satoken satoken 52428800
smoke shiro X-Session-Id 57671680
