#!/usr/bin/env bash
# POST /rotate-secret on the CLIENT – client calls server to rotate secret and stores the new one
# Usage: ./curl-rotate-secret.sh <keyId>
# KeyId is the keyId from a previous subscribe (see client logs: "Obtained secret for keyId: ...")
set -e
CLIENT_URL="${CLIENT_URL:-http://localhost:8081}"
KEY_ID="${1:?Usage: $0 <keyId>}"
curl -s -X POST "${CLIENT_URL}/rotate-secret?keyId=${KEY_ID}"
