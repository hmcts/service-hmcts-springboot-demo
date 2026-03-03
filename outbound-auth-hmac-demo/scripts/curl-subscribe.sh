#!/usr/bin/env bash
# POST /subscribe on the CLIENT – client then calls server (get secret, register, server sends callback)
# Usage: ./curl-subscribe.sh [name]
# Defaults: CLIENT_URL=http://localhost:8081, name=Guest
set -e
CLIENT_URL="${CLIENT_URL:-http://localhost:8081}"
NAME="${1:-Guest}"
curl -s -X POST "${CLIENT_URL}/subscribe?name=${NAME}"
