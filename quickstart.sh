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

# Check required Strava credentials
missing=()
[[ -z "${STRAVA_CLIENT_ID:-}" ]] && missing+=(STRAVA_CLIENT_ID)
[[ -z "${STRAVA_CLIENT_SECRET:-}" ]] && missing+=(STRAVA_CLIENT_SECRET)
if (( ${#missing[@]} > 0 )); then
  echo "ERROR: Missing required environment variables: ${missing[*]}" >&2
  echo "Set them in your shell before running, for example:" >&2
  echo "  export STRAVA_CLIENT_ID=your-client-id" >&2
  echo "  export STRAVA_CLIENT_SECRET=your-client-secret" >&2
  exit 2
fi

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
echo "Using STRAVA_CLIENT_ID=${STRAVA_CLIENT_ID}"

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
