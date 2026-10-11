# entra-node-login-demo

Sign in with Microsoft Entra and show who signed in. One button, one redirect, one page of claims.

Node, not Spring Boot - deliberately, because it mirrors the prototype this was taken from and keeps
the OAuth flow visible rather than hidden behind framework configuration. Three routes, three
dependencies - the third being [jose](https://github.com/panva/jose), because verifying a JWT
signature is the one step that should never be hand-rolled.

## What it does

1. `/` - a page with a **Sign in with Microsoft Entra** button
2. Cick on the sign in button and the browser is redirected to Entra
3. ( Entra handles the registration of user and sign in and then redirects back to /auth/callback )
4. `/auth/callback` - the app swaps the code for a JWT, verifies it, and reads the user's **oid**, **name** and **email**
5. We display the contents of the jwt to the now signed in user

## Run it

Node 20.19 or later (jose is ESM-only and is loaded with `require`). Everything but the secret is defaulted to the HMCTS sandbox tenant, so:

```bash
cd entra-node-login-demo
npm install
export ENTRA_CLIENT_SECRET=...     # from the sandbox app registration
npm start                          # http://localhost:3100
```

The secret is the only thing with no default, so it is the only thing that can be missing, and the
app refuses to start without it rather than failing later during the token exchange:

```
Cannot start: ENTRA_CLIENT_SECRET is not set.

  export ENTRA_CLIENT_SECRET=...   # from the Entra app registration
  npm start
```

**Export it. Do not put it in a file here** - exported values win over anything else, and a secret in
a file is a secret waiting to be committed.

### Port 3100, and why

It matches the redirect URI registered on the sandbox app registration. Entra only redirects back to
an address registered there, so on another port sign-in fails with `AADSTS50011` before the user sees
a login form.

That is also the port `hmcts-api-marketplace`'s prototype-kit uses, so the two cannot run at once.
Either stop that one, or add `http://localhost:<other-port>/auth/callback` to the app registration's
redirect URIs and set `PORT` to match.

### Pointing it at a different tenant

Every default can be overridden from the environment: `ENTRA_CLIENT_ID`, `ENTRA_AUTHORIZE_ENDPOINT`,
`ENTRA_TOKEN_ENDPOINT`, `ENTRA_REDIRECT_URI`, `PORT`. The two endpoints come from the tenant's
`/.well-known/openid-configuration`.

That document's own address is derived from the token endpoint (`.../oauth2/v2.0/token` becomes
`.../v2.0/.well-known/openid-configuration`), and `ENTRA_DISCOVERY_ENDPOINT` overrides it for a
tenant laid out differently. The app reads the issuer and signing keys from it.

## What it logs

Each route as it is hit, then the claims:

```
-> GET /auth/entra
   redirecting to Entra  state=de349f5f...  prompt=none
-> GET /auth/callback
   back from Entra  code=present  state=de349f5f...  error=none
   token exchange ok
   id token verified
   claims  oid=11111111-2222-3333-4444-555555555555  name=Ada Lovelace  email=ada@example.net
```

The same `state` either side is the CSRF check passing, and the quickest way to see a failed sign-in
for what it is. Three things are deliberately absent: the request path is logged without its query
string, because the callback's carries the authorization code; the code is logged as `present` or
`MISSING` rather than by value; and the ID token is never logged at all, only the claims read out of
it.

## The flow

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant A as This app<br/>(localhost:3000)
    participant E as Microsoft Entra

    B->>A: GET /auth/entra
    A->>A: generate random state and nonce,<br/>set httpOnly cookies
    A-->>B: 302 to Entra /authorize<br/>client_id, response_type=code,<br/>redirect_uri, scope, state, nonce

    B->>E: GET /oauth2/v2.0/authorize
    Note over B,E: The user authenticates with Microsoft.<br/>This app sees none of it.
    E-->>B: 302 to /auth/callback?code=...&state=...

    B->>A: GET /auth/callback
    A->>A: state == cookie? (CSRF check)

    rect rgba(128,128,128,0.12)
    Note over A,E: Back channel - server to server, no browser
    A->>E: POST /oauth2/v2.0/token<br/>code + client_secret<br/>grant_type=authorization_code
    E-->>A: id_token (JWT)
    end

    A->>E: GET discovery + keys (once, cached)
    A->>A: verify signature, alg, iss, aud,<br/>exp, nonce -> oid, name, email
    A-->>B: the signed-in page
```

Three things are worth knowing before the detail:

1. **The user's password never reaches this application.** They type it on Microsoft's own pages.
   This app never sees it, never stores it, and could not leak it.
2. **The browser never carries a token.** It carries a short-lived, single-use *authorization code*.
   The JWT is fetched separately, server to server. This distinction is the whole security design,
   and it is the thing most often misread.
3. **Entra knows who you are, and nothing else.** It holds `oid`, name, email and the credential. It
   has no concept of organisation, role, team or ownership - those belong in our database, keyed by `oid`.

## Two channels

**Front channel** - through the browser, visible in the address bar, in history, in any proxy log.
It carries only the authorization code.

**Back channel** - a direct HTTPS request from the Node process to Microsoft. It carries the token.

`response_type=code` is what selects this. The alternative, the old implicit flow
(`response_type=token`), returned the token itself into the browser's URL, where it would land in
browser history, server logs and `Referer` headers. The authorization code flow exists to avoid
that, and it is why the code arriving at `/auth/callback` is useless to anyone who steals it:
redeeming it also needs the client secret, which only the server has.

## Step by step

### `/auth/entra` - starting the flow

Generates 16 random bytes each for `state` and `nonce`, stores both in `httpOnly` cookies, and
redirects to the tenant's authorize endpoint with `client_id`, `response_type=code`, `redirect_uri`,
`scope=openid profile email`, that `state` and that `nonce`.

`state` is the CSRF defence for the round trip. Because the callback is a plain GET that anyone can
navigate a victim's browser to, the cookie is what proves *this browser* started *this* flow. A
callback whose `state` does not match the cookie is rejected.

`nonce` does a different job. Entra copies it into the ID token, so it ties the token itself to this
browser's sign-in. `state` protects the redirect; `nonce` protects the token, so a token from some
other sign-in cannot be replayed into this one.

`?prompt=login` is allow-listed for local demos: it makes Microsoft show its form even when the
browser already has a session, which is otherwise why sign-in appears to "just work" with no way to
try a different account.

### `/auth/callback` - the code comes back

Microsoft redirects the browser back with `?code=...&state=...`, or `?error=...`. Before anything
else: no `error`, and `state` matches the cookie.

Note what has *not* happened yet. At this point the app has a code and nothing else - no token, no
claims, no idea who the user is.

### Redeeming the code

A server-side `POST` to the token endpoint with `grant_type=authorization_code`, carrying the code,
the `redirect_uri` it was issued for, and the client secret. **This is where the JWT arrives** -
`id_token` in the response body, over TLS, direct from Microsoft to the Node process.

### Verifying the token

A JWT is three base64url segments separated by dots: `header.payload.signature`. Anyone can decode
the payload, and anyone can write one, so a decoded-but-unverified JWT is forgeable by anyone who can
type base64. Only the signature makes a claim trustworthy.

So before a single claim is read, `verifiedClaims` in `entra/client.js` checks:

| Check | Why |
|---|---|
| **signature** against the tenant's published keys, picked by the token's `kid` | only Entra holds the private key |
| **`alg` is `RS256`**, from our allow-list, never from the token's own header | blocks `alg: none` and the HS256 key-confusion forgery |
| **`iss`** is this tenant, as its discovery document names it | a token from another tenant is not ours |
| **`aud`** is this app's client ID | a token issued to some other app is not for us |
| **`exp` / `nbf`** are in date, with a minute of clock skew | old tokens expire |
| **`nonce`** matches this browser's cookie | a token from another sign-in cannot be replayed |

The issuer and the key-set address come from the tenant's discovery document, fetched once. That
matters: on the sandbox tenant the issuer is `https://d44f885c-....ciamlogin.com/...`, not the
`hmctsextsbox.ciamlogin.com` host the endpoints use, so a hard-coded guess would be wrong. jose
caches the keys and refetches when a token names a `kid` it has not seen, which is what happens when
Entra rotates them.

Then the payload yields `oid`, `name` and `email`, or `emails[0]`, because External ID returns an
array in some configurations.

Strictly, the signature is belt and braces on this one path: the token came straight off a TLS
connection to the token endpoint, authenticated with our own client secret, and OIDC Core 3.1.3.7
allows that TLS connection to stand in for the signature. It is checked anyway. Once the keys are
cached it costs nothing, and it means the same function is safe for a token from anywhere - a
browser, an API caller - where that excuse does not apply.

A token that fails any check gets a `401` and an "ID token rejected" page naming the reason, and none
of its claims are shown.

## What `oid` is

**Object ID** - the GUID of the user's object in the Entra directory. The primary key of the row
representing that human in the tenant.

| Property | Consequence |
|---|---|
| **Immutable** | Never changes for the life of the account |
| **Survives rename** | Name change, email change - same `oid` |
| **Tenant-wide** | Every application in the tenant sees the *same* `oid` for the same person |

### `oid` vs `sub`

An ID token carries both, and they are not interchangeable. `sub` is also a stable identifier, but
it is **pairwise** - derived per `(user, application)`. The same person signing into two
applications in the same tenant gets two *different* `sub` values. That is deliberate: it stops two
applications correlating their users by comparing identifiers.

`oid` is the opposite: the same value everywhere in the tenant. That makes it the right thing to key
your own data on - not email, which changes, and not `sub`, which a second service would compute
differently for the same person.

## What a real integration must change

This demo shows the flow. One thing is deliberately not production-shaped:

**Carry identity in a real session.** This demo renders the claims and forgets them, which is why it
needs no session at all. Anything that keeps a user signed in needs one - and it should hold the
`oid`, not the name or email.

In Spring Boot, `spring-boot-starter-oauth2-client` does that for you, along with the `state` and
`nonce` handling, the token exchange and the verification - see `entra-auth-demo` in this repo for the resource-server
half. This demo hand-rolls them precisely so the steps are visible.

## The tests

`npm test` uses Node's built-in test runner, with [supertest](https://github.com/ladjs/supertest) to
drive the app. Entra is a stub HTTP server on an ephemeral port rather than a mocked `fetch`, so the
real requests in `entra/client.js` - token, discovery and keys - are exercised. Its ID tokens are
really signed, with an RSA key pair generated for each run.

They cover the round trip as a browser performs it - start at `/auth/entra`, carry the cookies, come
back with the code - and three refusals of the callback: a `state` that does not match the cookie, no
cookie at all, and an `error` from Entra.

Then seven tokens that must not be believed, each failing exactly one check: signed by another key,
unsigned (`alg: none`), signed with a shared secret (HS256), issued for another app, issued by
another tenant, expired, and carrying another sign-in's nonce.

## In CI

`.github/workflows/run-tests.yml` runs `./gradlew clean test` for each Gradle module in its matrix.
This module has no Gradle build, so it has its own `Test-Node-Modules` job, which runs `npm ci` and
`npm test`.
