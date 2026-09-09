# entra-emulator-demo

A service layer for **Microsoft Entra directory management and token acquisition**, developed against
[Entra Local](https://github.com/cmaneu/entra-local) — an MIT-licensed Entra ID emulator that runs in Docker.

Registering an application, minting its secret and then exchanging that secret for an access token normally
needs a real tenant and a portal round trip. Here the whole cycle runs offline, in a container, in a test.

## What this demo shows

- Acquiring an app-only access token with the OAuth2 **client credentials** grant, against
  `{baseUrl}/{tenantId}/oauth2/v2.0/token` — the same request a real tenant answers
- Provisioning an application registration, its `appIdUri`, its app roles and a client secret
- Creating directory users
- The full round trip: **create an application → mint its secret → request a token as that application**,
  proven in `EntraLocalRoundTripTest` against the real emulator image via Testcontainers
- Keeping the emulator-only surface (`EntraAdminClient`) separate from the surface that also works against
  real Entra (`EntraTokenClient`)

## Tokens the emulator issues

```json
{
  "iss": "https://localhost:8443/11111111-1111-1111-1111-111111111111/v2.0",
  "aud": "api://amp-1106-job",
  "azp": "d99d618f-6176-431e-ba3c-e7d9eceb9036",
  "appid": "d99d618f-6176-431e-ba3c-e7d9eceb9036",
  "tid": "11111111-1111-1111-1111-111111111111",
  "roles": ["app.read"],
  "ver": "2.0"
}
```

Real RS256 signatures, verifiable against the emulator's JWKS endpoint, with the v2.0 claim shape.
Two claims a real tenant issues are absent: `oid` (the emulator reuses `sub`) and `azpacr`.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/entra/tenant` | Tenant id and the derived issuer, token and discovery endpoints |
| GET | `/api/entra/applications` | List application registrations |
| POST | `/api/entra/applications` | Create an application registration |
| POST | `/api/entra/applications/{id}/secrets` | Add a client secret — returned in clear **once** |
| POST | `/api/entra/applications/{id}/roles` | Add an application app role |
| POST | `/api/entra/daemon-applications` | Create application + `appIdUri` + roles + secret in one call |
| GET | `/api/entra/users` | List directory users |
| POST | `/api/entra/users` | Create a directory user |
| POST | `/api/entra/token` | Client credentials token for a client id / secret / resource |

## Running with Docker Compose

```bash
./entra-emulator-demo/gradlew -p entra-emulator-demo bootJar

docker compose -f entra-emulator-demo/docker/docker-compose.yml up --build
```

This starts `entra-local` on `8443` (its web portal is on the same port) and `api` on `8080`.

Provision a daemon application and take a token as that application:

```bash
CREDS=$(curl -s -X POST http://localhost:8080/api/entra/daemon-applications \
  -H 'Content-Type: application/json' \
  -d '{"displayName":"AMP-1106 Job","appIdUri":"api://amp-1106-job","appRoles":["app.read"]}')

curl -s -X POST http://localhost:8080/api/entra/token \
  -H 'Content-Type: application/json' \
  -d "$(echo "$CREDS" | python3 -c 'import sys,json; a=json.load(sys.stdin); print(json.dumps({"clientId":a["clientId"],"clientSecret":a["clientSecret"],"resourceUri":a["appIdUri"]}))')"
```

Or talk to the emulator directly, in the shape of the real Entra call:

```bash
curl -sk -X POST https://localhost:8443/11111111-1111-1111-1111-111111111111/oauth2/v2.0/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=client_credentials' \
  --data-urlencode 'client_id=<app id>' \
  --data-urlencode 'client_secret=<secret>' \
  --data-urlencode 'scope=api://amp-1106-job/.default'
```

## Pointing at a real tenant

Token acquisition is configuration-only:

```bash
ENTRA_BASE_URL=https://login.microsoftonline.com
ENTRA_TENANT_ID=<your tenant id>
ENTRA_TRUST_SELF_SIGNED=false
```

`EntraTokenClient` then talks to the real `login.microsoftonline.com/{tenantId}/oauth2/v2.0/token`
with no code change. **`EntraAdminClient` does not carry over** — the emulator's `/admin/api` surface
is its own, and the real equivalent is Microsoft Graph (`POST /v1.0/applications`, `POST /v1.0/users`),
which has different request and response shapes.

## Two things the emulator cannot do

- **Create a tenant.** Entra Local hosts exactly one tenant, fixed by its `TENANT_ID` environment
  variable. Neither can real Entra via an API — tenants are created in the portal. `EntraDirectoryService.tenant()`
  therefore reports the tenant rather than creating one.
- **Serve writable Microsoft Graph.** Its Graph surface is read-only (`/me`, `/users`, `/groups`).

## Issuer hostname in Docker

Entra Local stamps `iss` from its own public origin, which defaults to `https://localhost:8443`. In Compose
the API container reaches it as `entra-local:8443`, so an issued token says `localhost` while the configured
issuer says `entra-local`. Nothing here validates `iss`, so it does not bite — but a resource server that does
will reject the token. Set the emulator's `PUBLIC_ORIGIN` (or `ISSUER`) to the hostname clients use, and reach
it under that same name from every side.

## Certificates

Entra Local is HTTPS-only with an auto-generated self-signed certificate, so `entra.trust-self-signed`
defaults to `true` for local and Docker use. Against a real tenant set it to `false` — the JDK already
trusts Microsoft's CA, and leaving it on would disable certificate checking on a production endpoint.

## Why this emulator

Both [cmaneu/entra-local](https://github.com/cmaneu/entra-local) and
[calvinchengx/entra-emulator](https://github.com/calvinchengx/entra-emulator) were tested and both complete
the create-application → token round trip with near-identical admin APIs. Entra Local was chosen for its
deterministic tenant and seed-application ids, which make test fixtures stable. Its image is amd64-only,
so it runs under emulation on Apple Silicon; the Go alternative is native arm64 and swapping is an image
change. The image is pinned by digest in `docker-compose.yml` because `:latest` moves.

The emulator is a development tool. It trades security for convenience and must never be exposed beyond
a developer machine or CI job.

## Project structure

```
entra-emulator-demo/
├── build.gradle
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml                    Entra Local (pinned by digest) + this API
└── src/
    ├── main/java/uk/gov/hmcts/amp/entra/emulator/
    │   ├── client/EntraAdminClient.java      Emulator-only directory management
    │   ├── client/EntraTokenClient.java      Client credentials — works against real Entra
    │   ├── config/EntraProperties.java       baseUrl + tenantId derive every endpoint
    │   ├── config/EntraRestClientConfig.java Optional self-signed trust
    │   ├── controller/                       HTTP surface for the demo
    │   ├── model/                            Records for applications, secrets, users, tokens
    │   └── service/                          EntraDirectoryService, EntraTokenService
    └── test/java/uk/gov/hmcts/amp/entra/emulator/
        ├── client/                           WireMock tests, no Docker needed
        ├── integration/EntraLocalRoundTripTest.java   Testcontainers against the real image
        └── service/                          Service-layer unit tests
```
