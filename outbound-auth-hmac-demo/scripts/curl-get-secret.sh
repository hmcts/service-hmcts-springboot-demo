#!/usr/bin/env bash
# GET /secret on the CLIENT – returns keyId and secret from client's local store only (no server call)
# Usage: ./curl-get-secret.sh <keyId>
# KeyId is from a previous subscribe response (e.g. ./curl-subscribe.sh Alice)
set -e
CLIENT_URL="${CLIENT_URL:-http://localhost:8081}"
KEY_ID="${1:?Usage: $0 <keyId>}"
curl -s -X GET "${CLIENT_URL}/secret?keyId=${KEY_ID}" -H "Accept: application/json" | jq . 2>/dev/null || curl -s -X GET "${CLIENT_URL}/secret?keyId=${KEY_ID}" -H "Accept: application/json"
