#!/usr/bin/env bash
# The same journey as the page, from a terminal, with a bearer token instead of a browser sign-in.
# Needs the stand-ins and the app running (run-demo.sh). Needs curl and python3.
set -euo pipefail

API=http://localhost:8080
ENTRA=http://localhost:8090
STAND_IN=http://localhost:8099
bold() { printf '\n\033[1m%s\033[0m\n' "$*"; }
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
call() { # method path [body]
  curl -s -X "$1" "$API$2" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    ${3:+-d "$3"} -w '\n%{http_code}'
}

curl -s -X DELETE "$STAND_IN/__admin/requests" >/dev/null

bold "1. Sign in with Entra (a token from the stand-in)"
TOKEN=$(curl -s -X POST "$ENTRA/entra/token" \
  -d 'grant_type=client_credentials&client_id=demo-script&client_secret=x&scope=openid' | json "d['access_token']")
call GET /api/me | sed '$d' | json "'   signed in as %s (%s)' % (d['name'], d['via'])"

bold "2. Register an application: Entra gives it a Client ID and a Client Secret"
CREATED=$(call POST /api/applications '{"name":"Journey app"}' | sed '$d')
APP=$(echo "$CREATED" | json "d['application']['id']")
echo "$CREATED" | json "'   Client ID:     ' + d['application']['clientId']"
echo "$CREATED" | json "'   Client Secret: ' + d['clientSecret'] + '   (shown once)'"

bold "3. Connect it to an API: APIM gives it a Subscription Key"
call POST "/api/applications/$APP/apis" '{"apiId":"hearing-results"}' | sed '$d' \
  | json "'\n'.join('   %s -> %s' % (s['apiId'], s['subscriptionKey']) for s in d['subscriptions'])"

bold "4. An API that is not on offer is refused"
call POST "/api/applications/$APP/apis" '{"apiId":"not-an-api"}' | tail -1 | sed 's/^/   HTTP /'; echo

bold "5. Remove the API, then delete the application: both taken away in APIM and Entra"
call DELETE "/api/applications/$APP/apis/hearing-results" | tail -1 | sed 's/^/   remove API: HTTP /'; echo
call DELETE "/api/applications/$APP" | tail -1 | sed 's/^/   delete application: HTTP /'; echo

bold "What the app asked Graph and APIM for"
curl -s "$STAND_IN/__admin/requests" | python3 -c "
import json, sys
for r in sorted(json.load(sys.stdin)['requests'], key=lambda r: r['request']['loggedDate']):
    q = r['request']
    print('   %-6s %-84s -> %s' % (q['method'], q['url'].split('?')[0][:84], r['response']['status']))"
