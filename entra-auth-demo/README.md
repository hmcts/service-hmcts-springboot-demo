# entra-auth-demo

Demonstrates a Spring Boot 4 application configured as an **OAuth2 Resource Server** that validates Microsoft Entra (Azure AD) bearer tokens. In the Docker Compose setup, a [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server) replaces real Entra so the demo runs entirely offline.

## What this demo shows

- Spring Security OAuth2 Resource Server configured to validate JWTs via OIDC discovery (`issuer-uri`)
- Stateless session management — no HTTP sessions, every request must carry a valid Bearer token
- Public endpoint (`/actuator/health`) alongside protected endpoints (`/api/hello`, `/api/me`)
- JWT claims extraction: subject, tenant ID (`tid`), object ID (`oid`), and roles
- `mock-oauth2-server` standing in for Microsoft Entra in local/Docker environments
- Integration tests using `spring-security-test` JWT post-processors (no real token required)

## Endpoints

| Method | Path | Auth required | Description |
|--------|------|--------------|-------------|
| GET | `/actuator/health` | No | Health check |
| GET | `/api/hello` | Yes (Bearer JWT) | Returns a greeting with subject, tenant ID, and roles from the token |
| GET | `/api/me` | Yes (Bearer JWT) | Returns key JWT claims: `sub`, `oid`, `tid`, `issuer`, `expiresAt` |

## Running with Docker Compose

Build the JAR first (from the repo root), then start the stack:

```bash
./entra-auth-demo/gradlew -p entra-auth-demo bootJar

docker compose -f entra-auth-demo/docker/docker-compose.yml up --build
```

This starts:
- `mock-oauth2-server` on port `8090` — acts as a local Entra issuer
- `api` on port `8080` — the Spring Boot resource server

The API waits for the mock server to pass its health check before starting.

## Getting a token and calling the API

```bash
# Obtain an access token from the mock OAuth2 server
TOKEN=$(curl -s -X POST http://localhost:8090/entra/token \
  -d "grant_type=client_credentials&client_id=demo&client_secret=secret&scope=api://hmcts-demo/.default" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Call the protected /api/hello endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/hello

# Call the /api/me endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me
```

Example `/api/hello` response:

```json
{
  "message": "Hello from HMCTS API",
  "subject": "demo-client",
  "tenantId": "test-tenant-id-0000",
  "roles": ["API.Read"]
}
```

## Swapping in real Microsoft Entra

Set the `ENTRA_ISSUER_URI` environment variable to your tenant's v2.0 issuer URI:

```bash
ENTRA_ISSUER_URI=https://login.microsoftonline.com/{tenantId}/v2.0
```

Spring Boot will automatically fetch the OIDC discovery document from `{issuer-uri}/.well-known/openid-configuration` and use the published JWKS endpoint to validate incoming tokens. No other code changes are needed.

For Docker Compose, override the environment variable on the `api` service or pass it via a `.env` file.

## Project structure

```
entra-auth-demo/
├── build.gradle                              Gradle build (Spring Boot + OAuth2 Resource Server)
├── README.md
├── docker/
│   ├── docker-compose.yml                    Starts mock-oauth2-server + API
│   ├── Dockerfile                            Builds the API image
│   └── mock-oauth2-config.json               Token configuration for mock-oauth2-server
└── src/
    ├── main/
    │   ├── java/uk/gov/hmcts/amp/entra/
    │   │   ├── EntraAuthDemoApplication.java  Spring Boot entry point
    │   │   ├── config/SecurityConfig.java     JWT resource server security filter chain
    │   │   └── controller/ProtectedController.java  /api/hello and /api/me endpoints
    │   └── resources/application.yml
    └── test/
        └── java/uk/gov/hmcts/amp/entra/
            └── ProtectedControllerTest.java   MockMvc tests using spring-security-test JWTs
```
