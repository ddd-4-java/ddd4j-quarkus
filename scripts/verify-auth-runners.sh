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

smoke() {
  local auth=$1 header=$2 port=$3 budget=$4
  local module="$repo_root/ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-$auth"
  local jar log pid ready=false alice bob size
  jar=$(find "$module/target" -maxdepth 1 -name '*-runner.jar' -print -quit)
  [[ -n "$jar" ]] || { echo "missing runner jar for $auth" >&2; exit 1; }
  size=$(wc -c < "$jar" | tr -d ' ')
  (( size <= budget )) || { echo "$auth runner size $size exceeds budget $budget" >&2; exit 1; }
  log=$(mktemp "/tmp/ddd4j-quarkus-auth-$auth.XXXXXX.log")
  java -Dquarkus.http.host=127.0.0.1 -Dquarkus.http.port="$port" -jar "$jar" >"$log" 2>&1 &
  pid=$!
  trap 'kill "$pid" 2>/dev/null || true; wait "$pid" 2>/dev/null || true; rm -f "$log"' RETURN
  for _ in $(seq 1 120); do
    if curl --fail --silent --show-error "http://127.0.0.1:$port/auth/status" >/tmp/ddd4j-auth-status.json 2>/dev/null; then ready=true; break; fi
    kill -0 "$pid" 2>/dev/null || { tail -100 "$log" >&2; return 1; }
    sleep 0.25
  done
  $ready || { tail -100 "$log" >&2; return 1; }
  jq -e '.login == false' /tmp/ddd4j-auth-status.json >/dev/null
  alice=$(curl -fsS -H 'Content-Type: text/plain' --data alice "http://127.0.0.1:$port/auth/login" | jq -er '.token')
  bob=$(curl -fsS -H 'Content-Type: text/plain' --data bob "http://127.0.0.1:$port/auth/login" | jq -er '.token')
  curl -fsS -H "$header: $alice" "http://127.0.0.1:$port/auth/status" | jq -e '.login == true' >/dev/null
  curl -fsS -H "$header: $alice" "http://127.0.0.1:$port/auth/me" | jq -e '.authenticated == true and .loginId == "alice"' >/dev/null
  curl -fsS -H "$header: $bob" "http://127.0.0.1:$port/auth/me" | jq -e '.authenticated == true and .loginId == "bob"' >/dev/null
  curl -fsS -H "$header: $alice" "http://127.0.0.1:$port/auth/me" | jq -e '.loginId == "alice"' >/dev/null
  curl -fsS -H "$header: $alice" "http://127.0.0.1:$port/auth/check/role?role=user" | jq -e '.has == true' >/dev/null
  curl -fsS -H "$header: $alice" "http://127.0.0.1:$port/auth/check/permission?permission=profile%3Aread" | jq -e '.has == true' >/dev/null
  curl -fsS -X POST -H "$header: $alice" "http://127.0.0.1:$port/auth/logout" | jq -e '.success == true' >/dev/null
  curl -fsS -H "$header: $alice" "http://127.0.0.1:$port/auth/status" | jq -e '.login == false' >/dev/null
  curl -fsS -X POST -H "$header: $bob" "http://127.0.0.1:$port/auth/logout" | jq -e '.success == true' >/dev/null
  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
  rm -f "$log"
  trap - RETURN
  echo "auth runner verified: $auth ($size/$budget bytes)"
}

smoke satoken satoken 18081 52428800
smoke shiro X-Session-Id 18082 57671680
