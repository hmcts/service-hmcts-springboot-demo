#!/usr/bin/env bash
# POST /api/notify on the SERVER – sends message to all subscribers (each signed with that subscriber's secret)
# Usage: ./curl-notify.sh [message]
# Defaults: BASE_URL=http://localhost:8080, message=Hello everyone
set -e
BASE_URL="${BASE_URL:-http://localhost:8080}"
MESSAGE="${1:-Hello everyone}"
curl -s -X POST "${BASE_URL}/api/notify" \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"${MESSAGE}\"}" \
  -w "\nHTTP %{http_code}\n"
