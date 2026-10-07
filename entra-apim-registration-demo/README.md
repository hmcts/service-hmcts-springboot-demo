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
The demo proves that **flow**. It does **not** prove Microsoft would accept the requests; for that you need real
credentials, which is a configuration change (the addresses and secrets in `application.yml`), not a code change.

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

## Going real

The same settings, with Microsoft's addresses and real credentials: an app registration with
`Application.ReadWrite.OwnedBy` for Graph, and a service principal that can manage subscriptions on the APIM
instance. The Entra sign-in becomes a normal tenant's `issuer-uri`. See `application.yml`.
