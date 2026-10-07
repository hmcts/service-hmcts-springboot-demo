# entra-apim-registration-demo

A small, self-contained demo of the API Marketplace's credential journey, runnable on a laptop with only Docker
and Java:

1. **Sign in with Microsoft Entra**
2. **Register an application** and get a **Client ID and Client Secret** from Entra (via Microsoft Graph)
3. **Connect it to an API** and get a **Subscription Key** for that API from Azure API Management
4. Remove the API, delete the application: both are taken away in APIM and Entra too

It mirrors what [service-api-marketplace](https://github.com/hmcts/service-api-marketplace) does, in a form small
enough to read in one sitting.

## What is real and what is a stand-in

Entra, Graph and APIM are Microsoft cloud services, so none of them runs in Docker. Two stand-ins do:

| | Stand-in | Gives you |
|---|---|---|
| Microsoft Entra | [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server) (as in `entra-auth-demo`) | a real OpenID Connect login page, and real signed tokens |
| Microsoft Graph and Azure API Management | [WireMock](https://wiremock.org/) | answers to the calls that create an application, add a secret and create a subscription, with fresh made-up values each time |

The app itself is real: the sign-in, the requests it makes, their order, what it keeps and what it cleans up.
The stand-ins show the **flow**; they do not prove Microsoft would accept the requests. For that, see
[Against the real thing](#against-the-real-thing): the same app has been run against the real Graph and the real
sandbox APIM, and doing so was not just a change of addresses (it needed a retry the stand-ins never asked for).

## Run it

You need Docker and Java 25. From the repo root:

```bash
./entra-apim-registration-demo/run-demo.sh
```

That starts the two stand-ins, waits for them, and starts the app. Then open <http://localhost:8080>:

1. **Sign in with Entra.** You land on the stand-in's login page; enter any username.
2. **Register** an application. The page shows the Client ID and the **Client Secret once**; it is not stored.
3. **Connect API** to one of the listed APIs. Its **Subscription Key** appears in the table.
4. **Remove** the API or **Delete application**.

| | |
|---|---|
| The app | <http://localhost:8080> |
| Every call the app made to Graph and APIM | <http://localhost:8099/__admin/requests> |
| The Entra stand-in's discovery document | <http://localhost:8090/entra/.well-known/openid-configuration> |

Stop the stand-ins with `docker compose -f entra-apim-registration-demo/docker/docker-compose.yml down`.

### Without a browser

`./entra-apim-registration-demo/journey.sh` (with the app running) does the same journey from a terminal, using a
bearer token from the stand-in instead of the browser login, and prints the calls the app made to Graph and APIM.

### Why the app is not in Docker

The browser and the app must both reach Entra at `http://localhost:8090`, because that is the issuer written into
its tokens. Running the app on your machine keeps that true for both with no extra setup.

## How it fits together

```
browser ──sign in──▶ mock-entra :8090            (authorization-code login, tokens)
   │
   └──▶ app :8080 ──token──▶ mock-entra :8090    (client credentials, to call Graph and Azure)
            │
            ├──▶ stand-in :8099 /graph/...       POST /applications, /servicePrincipals, .../addPassword
            └──▶ stand-in :8099 /arm/...         PUT/DELETE .../subscriptions/{name}
```

| Class | What it does |
|---|---|
| `SecurityConfig` | Two ways in, both from the same issuer: a browser session after Entra sign-in, or a bearer token. `/api` answers 401, not a redirect |
| `EntraGraphClient` | Create application → create service principal → add password (the secret); delete by Client ID |
| `ApimClient` | One PUT creates a subscription and returns its key; DELETE removes it |
| `RegistrationService` | The journey and its order: refuse what can be refused, ask Entra and APIM, then remember |
| `ApplicationController` | `/api/me`, `/api/applications`, `/api/applications/{id}/apis` |

Applications are kept in memory, so restarting the app forgets them (the stand-ins forget nothing it asked for;
see the request log). The Client Secret is never kept at all.

## Tests

```bash
./gradlew test
```

They need no Docker and no network: the requests are checked against a mock server, the journey against mocked
clients, and the HTTP surface (401 when not signed in, CSRF for a browser session, one user cannot see
another's application) with MockMvc.

## Against the real thing

The app is the same; the settings point at Microsoft. Nothing below is needed for the offline demo.

**What has been run for real** (7 October 2026): this app's own Graph and APIM clients, against the real External ID
tenant and the real sandbox APIM. Register an application, connect an API, remove it, delete the application: all
worked, and everything created was deleted and checked gone. And the app's sign-in was taken as far as Microsoft's
real `HMCTSEXTSBOX` login page.

**What it showed that the stand-ins did not.** Straight after an application is created, the real Graph refuses the
next calls about it for a few seconds: the service principal with `403 Authorization_RequestDenied`, the secret with a
`4xx`. The first real run failed on exactly that, so `EntraGraphClient` now retries those two calls (4 attempts, 2
seconds apart) and the tests cover it. The APIM management API also went on listing a deleted subscription for several
minutes.

**What has not.** A real user completing the sign-in (that needs your own account and password). And whether a token
from the application it creates is accepted by APIM, which sits in the corporate tenant while the application is in the
External ID tenant: no API was called with what was issued.

### Settings

Everything is overridable with `SPRING_APPLICATION_JSON` (shown with placeholders):

```json
{
  "spring": { "security": { "oauth2": {
    "client": {
      "registration": { "entra": { "client-id": "<the sign-in app>", "client-secret": "<its secret>", "scope": ["openid", "profile", "email"] } },
      "provider": { "entra": {
        "authorization-uri": "https://<tenant-name>.ciamlogin.com/<tenant-id>/oauth2/v2.0/authorize",
        "token-uri":         "https://<tenant-name>.ciamlogin.com/<tenant-id>/oauth2/v2.0/token",
        "jwk-set-uri":       "https://<tenant-name>.ciamlogin.com/<tenant-id>/discovery/v2.0/keys" } } },
    "resourceserver": { "jwt": { "jwk-set-uri": "https://<tenant-name>.ciamlogin.com/<tenant-id>/discovery/v2.0/keys" } } } } },
  "registration": {
    "entra": { "tokenUrl": "https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token",
               "graphBaseUrl": "https://graph.microsoft.com/v1.0",
               "clientId": "<an identity that may create applications>", "clientSecret": "<its secret>" },
    "apim": { "tokenUrl": "https://login.microsoftonline.com/<apim-tenant-id>/oauth2/v2.0/token",
              "armBaseUrl": "https://management.azure.com",
              "clientId": "<a service principal that may manage subscriptions>", "clientSecret": "<its secret>",
              "subscriptionId": "<azure subscription>", "resourceGroup": "<rg>", "serviceName": "<apim name>",
              "products": { "hearing-results": "<an APIM product id>" } } }
}
```

Two addresses differ, and both are real: **users sign in at `<tenant-name>.ciamlogin.com`** (the External ID
authority), while **the app-only token for Graph comes from `login.microsoftonline.com/<tenant-id>`**.

### Sign-in without editing the app registration

The marketplace's sign-in app is already registered for `http://localhost:3100/auth/callback`. This app's callback is
that path, so run it on that port and the real sign-in works as it is:

```bash
SERVER_PORT=3100 SPRING_APPLICATION_JSON='...' ./gradlew bootRun     # then open http://localhost:3100
```

The stand-in does not mind the path, so the offline demo is unaffected.

### The APIM credential

The service principal for APIM does not exist yet. Until it does, anything that needs a token for Azure Resource
Manager needs one from somewhere: `az account get-access-token --resource https://management.azure.com` gives you
your own, which is what the real runs used (through a small local shim answering the token request).
