#!/usr/bin/env bash

set -euo pipefail

# Quick launcher for Strava Activity Analyzer in local profile
# Usage:
#   ./quickstart.sh            # run with local profile
#   ./quickstart.sh --rebuild  # clean build then run
#   ./quickstart.sh --no-open  # don't auto-open browser

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

if [[ ! -f "gradlew" ]]; then
  echo "ERROR: gradlew not found. Please run from the project root." >&2
  exit 1
fi

# Resolve Strava credentials: prefer application-local.properties, then env vars

# Helper to extract a property value by key from a .properties file (ignores comments/whitespace)
prop_get() {
  local file="$1" key="$2"
  # Use awk to find first non-comment, non-empty matching line, split on first '=' and trim
  awk -v k="$2" '
    BEGIN{FS="="}
    /^[[:space:]]*#/ {next}
    /^[[:space:]]*$/ {next}
    {
      # Trim spaces around key and value
      key=$1; sub(/^[[:space:]]+|[[:space:]]+$/, "", key)
      if (key == k) {
        $1=""; val=substr($0, index($0, FS)+1)
        sub(/^[[:space:]]+/, "", val); sub(/[[:space:]]+$/, "", val)
        print val; exit
      }
    }
  ' "$file"
}

CRED_SOURCE=""
PROP_FILE="src/main/resources/application-local.properties"
CLIENT_ID=""
CLIENT_SECRET=""

if [[ -f "$PROP_FILE" ]]; then
  # Fail fast if file is not readable
  if [[ ! -r "$PROP_FILE" ]]; then
    echo "ERROR: $PROP_FILE exists but is not readable. Fix permissions (e.g. chmod 600) and try again." >&2
    exit 2
  fi
  # Prefer Spring Security OAuth keys
  CLIENT_ID=$(prop_get "$PROP_FILE" "spring.security.oauth2.client.registration.strava.client-id" || true)
  CLIENT_SECRET=$(prop_get "$PROP_FILE" "spring.security.oauth2.client.registration.strava.client-secret" || true)
  # Fallback to legacy keys if needed
  if [[ -z "$CLIENT_ID" ]]; then CLIENT_ID=$(prop_get "$PROP_FILE" "strava.clientId" || true); fi
  if [[ -z "$CLIENT_SECRET" ]]; then CLIENT_SECRET=$(prop_get "$PROP_FILE" "strava.clientSecret" || true); fi

  # Treat placeholders as empty
  if [[ "$CLIENT_ID" == "\${STRAVA_CLIENT_ID:"* || "$CLIENT_ID" == "your-client-id" ]]; then CLIENT_ID=""; fi
  if [[ "$CLIENT_SECRET" == "\${STRAVA_CLIENT_SECRET:"* || "$CLIENT_SECRET" == "your-client-secret" ]]; then CLIENT_SECRET=""; fi

  if [[ -n "$CLIENT_ID" && -n "$CLIENT_SECRET" ]]; then
    CRED_SOURCE="properties ($PROP_FILE)"
  fi
fi

if [[ -z "$CRED_SOURCE" ]]; then
  CLIENT_ID="${STRAVA_CLIENT_ID:-}"
  CLIENT_SECRET="${STRAVA_CLIENT_SECRET:-}"
  if [[ -n "$CLIENT_ID" && -n "$CLIENT_SECRET" ]]; then
    CRED_SOURCE="environment variables"
  fi
fi

if [[ -z "$CRED_SOURCE" ]]; then
  echo "" >&2
  echo "ERROR: Missing Strava credentials for local run." >&2
  if [[ -f "$PROP_FILE" ]]; then
    echo "Detected $PROP_FILE but it does not contain valid values for: " >&2
    echo "  - spring.security.oauth2.client.registration.strava.client-id" >&2
    echo "  - spring.security.oauth2.client.registration.strava.client-secret" >&2
    echo "The file likely still has placeholders like 'your-client-id'/'your-client-secret'." >&2
  else
    echo "$PROP_FILE was not found." >&2
  fi
  echo "" >&2
  echo "How to fix:" >&2
  echo "  Option A (preferred): create and edit $PROP_FILE" >&2
  echo "    cp src/main/resources/application.properties $PROP_FILE" >&2
  echo "    # then set real values:" >&2
  echo "    spring.security.oauth2.client.registration.strava.client-id=YOUR_STRAVA_CLIENT_ID" >&2
  echo "    spring.security.oauth2.client.registration.strava.client-secret=YOUR_STRAVA_CLIENT_SECRET" >&2
  echo "  Option B: set environment variables before running this script" >&2
  echo "    export STRAVA_CLIENT_ID=YOUR_STRAVA_CLIENT_ID" >&2
  echo "    export STRAVA_CLIENT_SECRET=YOUR_STRAVA_CLIENT_SECRET" >&2
  echo "" >&2
  echo "Tip: Visit https://www.strava.com/settings/api to obtain a Client ID and Client Secret." >&2
  exit 2
fi

# Export for Spring placeholders in application.properties if needed
export STRAVA_CLIENT_ID="$CLIENT_ID"
export STRAVA_CLIENT_SECRET="$CLIENT_SECRET"

SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-local}
REBUILD=false
OPEN_BROWSER=true

for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=true ;;
    --no-open) OPEN_BROWSER=false ;;
    -h|--help)
      sed -n '1,40p' "$0" | sed -n '1,20p' | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      exit 3
      ;;
  esac
done

CMD=("./gradlew")
if [[ "$REBUILD" == "true" ]]; then
  CMD+=(clean)
fi
CMD+=(bootRun "-Dspring-boot.run.profiles=${SPRING_PROFILES_ACTIVE}")

echo "Starting Strava Activity Analyzer with profile=${SPRING_PROFILES_ACTIVE}…"
echo "Credentials source: ${CRED_SOURCE}"
if [[ -n "${STRAVA_CLIENT_ID}" ]]; then
  echo "Using STRAVA_CLIENT_ID=${STRAVA_CLIENT_ID}"
else
  echo "Using STRAVA_CLIENT_ID is empty (unexpected)." >&2
fi

# macOS convenience: open browser once server is likely up
if $OPEN_BROWSER; then
  (
    sleep 4
    if command -v open >/dev/null 2>&1; then
      open "http://localhost:8080"
    fi
  ) &
fi

exec "${CMD[@]}"
