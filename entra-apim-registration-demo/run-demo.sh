#!/usr/bin/env bash
# Starts the Entra and Graph/APIM stand-ins in Docker, then the app. Open http://localhost:8080
#
#   ./entra-apim-registration-demo/run-demo.sh          (from the repo root, or from here)
#   docker compose -f docker/docker-compose.yml down    (to stop the stand-ins afterwards)
set -euo pipefail
cd "$(dirname "$0")"

docker compose -f docker/docker-compose.yml up -d

# The Entra image has no shell to run a healthcheck with, so wait for the two stand-ins from outside.
wait_for() {
  printf 'Waiting for %s' "$1"
  for _ in $(seq 1 60); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' -m 2 "$2" || true)" = "200" ]; then printf ' - up\n'; return 0; fi
    printf '.'; sleep 1
  done
  printf '\n%s did not come up. Its log:\n' "$1"; docker compose -f docker/docker-compose.yml logs --tail 20; exit 1
}
wait_for "the Entra stand-in" http://localhost:8090/entra/.well-known/openid-configuration
wait_for "the Graph/APIM stand-in" http://localhost:8099/__admin/health
echo
echo "Entra stand-in:  http://localhost:8090/entra/.well-known/openid-configuration"
echo "Graph/APIM:      http://localhost:8099/__admin/requests   (every call the app makes to them)"
echo "Starting the app on http://localhost:8080 ..."
exec ./gradlew bootRun
