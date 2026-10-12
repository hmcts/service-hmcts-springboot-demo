# entra-app-registration-demo

Registers a Microsoft Entra application and hands back a **Client ID** and **Client Secret** a
subscriber can use. The backend half of the credential journey: `entra-node-login-demo` signs a
person in, this registers an application for them.

The same three Graph calls proven in the `hmcts-api-marketplace` prototype and
[service-api-marketplace#95](https://github.com/hmcts/service-api-marketplace/pull/95), through the
Microsoft Graph SDK rather than hand-written HTTP - matching `apim-subscription-key-demo`, which uses
the Azure SDK for the same reason.

## The API

| | | |
|---|---|---|
| `POST` | `/applications` | register one; answers with its Client ID and Secret |
| `DELETE` | `/applications/{objectId}` | remove it, and with it the credentials |

```bash
curl -X POST localhost:8080/applications \
  -H 'Content-Type: application/json' -d '{"name":"Team Alpha"}'

curl -X DELETE localhost:8080/applications/<objectId>
```

## The three Graph calls

```java
graph.applications().post(application)                              // id (object id) + appId (client id)
graph.servicePrincipals().post(servicePrincipal)                    // the identity in the tenant
graph.applications().byApplicationId(id).addPassword().post(..)     // the secret, returned once
```

`com.microsoft.graph:microsoft-graph` with `ClientSecretCredential`, so the token is fetched and
refreshed by the SDK and nothing here handles one. Note `PasswordCredential.getKeyId()` returns a
`UUID`, not a `String`.

Four things in that are easy to get wrong:

- **`id` and `appId` are different.** The create response carries both: `id` is the directory object,
  `appId` is the Client ID. `addPassword` needs `id`; everything a subscriber uses needs `appId`.
- **The service principal is not optional.** The registration is a definition; the service principal
  is the identity. Without it `client_credentials` cannot get a token at all, so the credentials
  would be inert.
- **The secret is returned once.** There is no call that reads it back - which is why `POST
  /applications` cannot return one, and `addPassword` has to exist as a separate step.
- **The last two calls need retrying.** Graph's directory read replicas lag the write that created
  the application, so the very next call can 404 against a stale replica. Four attempts, two seconds
  apart. This appears only against the real Graph; no stand-in will ever show it to you.

Deleting the application removes its service principal too. It is addressed by **object id**, not
Client ID: the raw API accepts Graph's alternate key syntax `/applications(appId='..')`, but the SDK's
`byApplicationId` takes the object id, so that is what the caller keeps.

## Run it

```bash
cd entra-app-registration-demo
export ENTRA_ONBOARDING_CLIENT_SECRET=...
./gradlew bootRun
```

Then register one. This is the only moment the secret exists - Entra will not show it again:

```bash
curl -s -X POST localhost:8080/applications \
  -H 'Content-Type: application/json' \
  -d '{"name":"colin-test"}'
```

```json
{
  "objectId": "7c1d9f2e-4b58-4a31-9f0c-2e6d8a1b3c44",
  "clientId": "e9a4b17d-6f32-4c90-8d55-1a2b3c4d5e6f",
  "clientSecret": "shown once, never again",
  "keyId": "b3f8c21a-9d47-4e65-a0b2-7c9e1f4d8a33"
}
```

Both ids come back for a reason: `clientId` is what a subscriber authenticates with, `objectId` is how
the application is addressed again afterwards. Keep it, or you cannot remove what you just made.

```bash
curl -i -X DELETE localhost:8080/applications/7c1d9f2e-4b58-4a31-9f0c-2e6d8a1b3c44
```

```
HTTP/1.1 204 No Content
```

Deleting the application removes its service principal with it. These are real writes to the
`hmctsextsbox` tenant, so tidy up after a try - what you leave behind shows in the Entra portal under
App registrations.


The secret is the only setting with no default, so it is the only one that can be missing. Without it
the app refuses to start, through a Spring Boot `FailureAnalyzer` so the reason is the last thing on
screen rather than buried in a stack trace:

```
***************************
APPLICATION FAILED TO START
***************************

Description:
ENTRA_ONBOARDING_CLIENT_SECRET is not set, and it is the only setting with no default.

Action:
Export the amp-client-onboarding secret and start again:
    export ENTRA_ONBOARDING_CLIENT_SECRET=...
```

**SIGNIN and ONBOARDING are two different registrations**, which is why the variables say which:

| | | |
|---|---|---|
| `ENTRA_SIGNIN_CLIENT_*` | `aedd508a-...` | signs people in - `entra-node-login-demo` |
| `ENTRA_ONBOARDING_CLIENT_*` | `8f867c90-...` | calls Graph - this demo |

Only the second holds `Application.ReadWrite.OwnedBy`. Using the sign-in secret here authenticates
fine and then fails with `Authorization_RequestDenied`, which reads like a bug and is not one. Both
can sit in one shell without colliding, which is the point of the names.

| Setting | Default |
|---|---|
| `ENTRA_TENANT_ID` | the `hmctsextsbox` CIAM tenant |
| `ENTRA_ONBOARDING_CLIENT_ID` | `8f867c90-...`, the amp-client-onboarding-prototype identity |
| `ENTRA_ONBOARDING_CLIENT_SECRET` | *required* - no default, and never will have one |
| `ENTRA_GRAPH_BASE_URL` | `https://graph.microsoft.com/v1.0` |
| `ENTRA_LOGIN_BASE_URL` | `https://login.microsoftonline.com` |

Without the credential it answers `503` rather than failing at the first Graph call.

## What permissions the onboarding identity needs

One Microsoft Graph **application** permission: **`Application.ReadWrite.OwnedBy`**. It covers all
three calls, because the identity becomes the owner of what it creates.

This is checked, not assumed: the `amp-client-onboarding-prototype` identity in `hmctsextsbox` has
exactly that one permission granted, and the prototype works.

Note what `OwnedBy` does *not* cover, because it shapes what can be built next: it can only manage
applications it owns, and it cannot assign app roles. Granting a subscriber's application a role on
an API needs `AppRoleAssignment.ReadWrite.All`, which is **not granted**. So the credentials this
issues can authenticate, but nothing yet authorises them against a particular API - see below.

## What this does not do

Deliberately out of scope, and each is its own piece of work:

- **The API side.** For a token to be validated by APIM or a Spring Boot resource server, there must
  be an app registration *for the API*, with an Application ID URI and app roles, and the subscriber
  must be assigned one. Blocked on `AppRoleAssignment.ReadWrite.All`.
- **APIM.** Sandbox APIM today has no `validate-jwt` policy anywhere and every product requires a
  subscription key, so the gateway is gated on keys rather than tokens. `apim-subscription-key-demo`
  covers that half.
- **Persistence.** Nothing is stored. The secret is shown once and forgotten, the same behaviour
  Entra itself has.
