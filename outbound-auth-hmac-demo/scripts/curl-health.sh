#!/usr/bin/env bash
# GET /actuator/health – server health (default actuator endpoint)
set -e
BASE_URL="${BASE_URL:-http://localhost:8080}"
curl -s "${BASE_URL}/actuator/health" | jq . 2>/dev/null || curl -s "${BASE_URL}/actuator/health"
