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
| `GET` | `/applications` | every application this identity owns, oldest first |
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
  apart. This appears only against the real Graph; no mock Entra will ever show it to you.

A registration that fails after the application exists takes it away again. The service principal and
password steps come after the application is created, so if they exhaust their retries the
application would otherwise be left in the directory with no secret and nobody holding its id - which
is how orphans accumulate. A failure to clean up is reported as a suppressed exception rather than
replacing the failure that caused it: the caller needs to know registration failed, not that tidying
up afterwards did.

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

List what is there, to find what is worth removing. Oldest first, and never with a secret - Entra
cannot give one back:

```bash
curl -s localhost:8080/applications
```

```json
[
  { "objectId": "...", "clientId": "...", "displayName": "an-old-test", "created": "2026-07-06T09:12:44Z" },
  { "objectId": "...", "clientId": "...", "displayName": "colin-test",  "created": "2026-10-12T08:01:02Z" }
]
```

Sorted in Java, not by Graph: `$orderby` on applications is refused outright with *"Sorting not
supported for 'Application'"* unless the request opts into advanced queries. A mock will not tell
you that - it serves whatever you stub, which is how this passed its tests and then 500'd against the
real tenant.

```bash
curl -i -X DELETE localhost:8080/applications/7c1d9f2e-4b58-4a31-9f0c-2e6d8a1b3c44
```

```
HTTP/1.1 204 No Content
```

Deleting the application removes its service principal with it. These are real writes to the
`hmctsextsbox` tenant, so tidy up after a try - what you leave behind shows in the Entra portal under
App registrations.


The secret is the only setting with no default, so it is the only one that can be missing. It is
`@NotBlank` on `EntraProperties`, so Spring Boot's own binding diagnostics report it at startup -
rather than a 401 from Graph on the first registration:

```
***************************
APPLICATION FAILED TO START
***************************

Description:
Binding to target ...EntraProperties failed:

    Property: entra.onboardingClientSecret
    Value: ""
    Reason: set ENTRA_ONBOARDING_CLIENT_SECRET - the amp-client-onboarding secret, not the
            sign-in application's that entra-node-login-demo uses
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
| `ENTRA_MAX_ATTEMPTS` | `10` - Entra replication is slow, and how slow varies by tenant |
| `ENTRA_RETRY_DELAY_MILLIS` | `500` |
| `ENTRA_GRAPH_BASE_URL` | `https://graph.microsoft.com/v1.0` |
| `ENTRA_LOGIN_BASE_URL` | `https://login.microsoftonline.com` |

Without the credential it answers `503` rather than failing at the first Graph call.

## Using the credentials

The Client ID and Secret are what a subscriber authenticates with. They work immediately, because the
service principal was created alongside them:

Take them straight off the registration:

```bash
read -r CLIENT_ID CLIENT_SECRET < <(curl -s -X POST localhost:8080/applications \
  -H 'Content-Type: application/json' -d '{"name":"colin-test"}' \
  | jq -r '"\(.clientId) \(.clientSecret)"')
echo "ClientId:$CLIENT_ID ClientSecret:$CLIENT_SECRET"
```

```bash
echo "ClientId:$CLIENT_ID ClientSecret:$CLIENT_SECRET"
curl -s -X POST "https://login.microsoftonline.com/d44f885c-4fac-47bf-afde-d7d861ec4d7b/oauth2/v2.0/token" \
  -d "client_id=$CLIENT_ID" \
  --data-urlencode "client_secret=$CLIENT_SECRET" \
  -d "scope=https://graph.microsoft.com/.default" \
  -d "grant_type=client_credentials"
```

`--data-urlencode` on the secret, not `-d`: Entra secrets contain `~`, `.` and sometimes `+` or `=`,
and an unencoded one is silently mangled into `AADSTS7000215: Invalid client secret provided`.

That returns a token - but look at what is in it:

```
aud=https://graph.microsoft.com   roles=(none)
```

**Authenticated, but authorised for nothing.** Use it and Graph says so:

```
Authorization_RequestDenied - Insufficient privileges to complete the operation.
```

This is the thing worth understanding about `client_credentials`: there is no user, so there are no
delegated scopes and no `scp` claim. Authorisation arrives as `roles`, and `roles` is empty until the
application is **assigned an app role** on the API it wants to call.

For one of your own APIs the scope would be `api://<api-app-id>/.default`, and without that API
registration existing the request does not even get as far as permissions:

```
AADSTS500011: The resource principal named api://... was not found in the tenant
```

So a complete journey needs three things this demo does not do:

1. an app registration **for the API**, with an Application ID URI and app roles
2. an **app role assignment** linking this application's service principal to the API's
3. something validating the token - APIM `validate-jwt`, or a Spring Boot resource server

Step 2 needs the Graph permission `AppRoleAssignment.ReadWrite.All`, which the onboarding identity
does not hold - see below.

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
