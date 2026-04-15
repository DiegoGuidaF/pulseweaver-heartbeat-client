#!/bin/sh
set -eu

log() {
  echo "[heartbeat] $(date -u +%Y-%m-%dT%H:%M:%SZ) $*"
}

# Validate required environment variables
missing=""
[ -z "${HEARTBEAT_URL:-}" ]              && missing="$missing HEARTBEAT_URL"
[ -z "${HEARTBEAT_API_KEY:-}" ]          && missing="$missing HEARTBEAT_API_KEY"
[ -z "${HEARTBEAT_INTERVAL_SECONDS:-}" ] && missing="$missing HEARTBEAT_INTERVAL_SECONDS"

if [ -n "$missing" ]; then
  log "ERROR: missing required environment variables:$missing"
  exit 1
fi

# Validate HEARTBEAT_INTERVAL_SECONDS is a positive integer
case "$HEARTBEAT_INTERVAL_SECONDS" in
  ''|*[!0-9]*)
    log "ERROR: HEARTBEAT_INTERVAL_SECONDS must be a positive integer, got: '${HEARTBEAT_INTERVAL_SECONDS}'"
    exit 1 ;;
esac
if [ "$HEARTBEAT_INTERVAL_SECONDS" -eq 0 ]; then
  log "ERROR: HEARTBEAT_INTERVAL_SECONDS must be greater than zero"
  exit 1
fi

# Optional: override the full heartbeat path (default: /api/v1/heartbeat)
HEARTBEAT_ENDPOINT="${HEARTBEAT_ENDPOINT:-/api/v1/heartbeat}"

log "Starting — server: $HEARTBEAT_URL, endpoint: $HEARTBEAT_ENDPOINT, interval: ${HEARTBEAT_INTERVAL_SECONDS}s"

while true; do
  log "Sending heartbeat..."

  RAW=$(curl \
    --silent \
    --show-error \
    --fail-with-body \
    --connect-timeout 5 \
    --max-time 10 \
    --retry 1 \
    --retry-delay 2 \
    --write-out "\n%{http_code}" \
    --header "X-API-Key: $HEARTBEAT_API_KEY" \
    --request POST \
    "${HEARTBEAT_URL}${HEARTBEAT_ENDPOINT}" 2>&1) || true

  HTTP_STATUS=$(echo "$RAW" | tail -1)
  BODY=$(echo "$RAW" | head -n -1)

  log "status=$HTTP_STATUS body=$BODY"

  sleep "$HEARTBEAT_INTERVAL_SECONDS"
done
