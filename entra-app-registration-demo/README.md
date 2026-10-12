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
| `GET` | `/applications/{clientId}` | one application, and the API roles it holds |
| `POST` | `/applications/{clientId}/roles` | let it call one of the platform's APIs |
| `DELETE` | `/applications/{clientId}/roles/{role}` | take that away again |
| `POST` | `/applications` | register one; answers with its Client ID and Secret |
| `DELETE` | `/applications/{clientId}` | remove it, and with it the credentials |

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


```bash

# Create an application and store the client id for use in future curl commands
# This is the only time the secret exists - Entra will not show it again
read -r CLIENT_ID CLIENT_SECRET < <(curl -s -X POST localhost:8080/applications \
  -H 'Content-Type: application/json' -d '{"name":"colin-test2"}' \
  | jq -r '"\(.clientId) \(.clientSecret)"')
echo "ClientId:$CLIENT_ID ClientSecret:$CLIENT_SECRET"

# List all applications, oldest first
curl -s localhost:8080/applications | jq .

# To see what roles are assigned to an application - empty for a new one
curl -s localhost:8080/applications/$CLIENT_ID | jq .

# Use the previously created application to get an Entra token to use with an API
TENANT=d44f885c-4fac-47bf-afde-d7d861ec4d7b                 # hmctsextsbox
API_CLIENT_ID=30288840-e345-4543-99ee-f9253d789339           # hmcts-api-marketplace
TOKEN=$(curl -s -X POST "https://login.microsoftonline.com/$TENANT/oauth2/v2.0/token" \
  -d "client_id=$CLIENT_ID" --data-urlencode "client_secret=$CLIENT_SECRET" \
  -d "scope=$API_CLIENT_ID/.default" -d "grant_type=client_credentials" | jq -r .access_token)

# Decode the token to see its claims - aud is the API it is for, roles what it may do there
echo "$TOKEN" | jq -R 'split(".")[1] | @base64d | fromjson | {aud, roles, azp}'

# Assign a role to an application, so APIM will accept its token
curl -i -X POST localhost:8080/applications/$CLIENT_ID/roles \
  -H 'Content-Type: application/json' -d '{"role":"slc.read"}'

# Take a role away from an application
curl -i -X DELETE localhost:8080/applications/$CLIENT_ID/roles/slc.read

# Call the slc API through sbox APIM with that token
# Needs BOTH: the JWT for validate-jwt, and a subscription key because the product requires one
curl -i "https://sps-api-mgmt.sandbox.api.hmcts.net/amp/slc/case/$CASE_URN/courtschedule" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Ocp-Apim-Subscription-Key: $SUBSCRIPTION_KEY"

# Delete an application by clientId - its service principal goes with it
curl -i -X DELETE localhost:8080/applications/$CLIENT_ID
```

These are real writes to the `hmctsextsbox` tenant, so tidy up after a try - what you leave behind
shows in the Entra portal under App registrations.


Registering one answers with the only copy of the secret there will ever be:

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

Listing shows what is there, oldest first, and never with a secret - Entra cannot give one back:

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

Deleting an application answers `204`, and removes its service principal with it.


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
| `ENTRA_MAX_ATTEMPTS` | `20` - 20 x 500ms, because Entra replication really does take seconds |
| `ENTRA_RETRY_DELAY_MILLIS` | `500` |
| `ENTRA_GRAPH_BASE_URL` | `https://graph.microsoft.com/v1.0` |
| `ENTRA_LOGIN_BASE_URL` | `https://login.microsoftonline.com` |

Without the credential it answers `503` rather than failing at the first Graph call.

## Using the credentials

The Client ID and Secret are what a subscriber authenticates with. They work immediately, because the
service principal was created alongside them:

That returns an `access_token`, which is a JWT - three dot-separated parts, only the middle one
worth reading:

```
eyJ0eXAiOiJKV1QiLCJub25jZSI6Ik...   header
eyJhdWQiOiJodHRwczovL2dyYXBoLm...   claims     <- this one
cTftAhBDT4G4-jDibUkAGCjw0mOFy_...   signature
```

The decode above is worth unpacking:

| | |
|---|---|
| `-R` | read the token as a raw string, not as JSON |
| `split(".")[1]` | take the middle part, the claims |
| `@base64d` | decode it - this is base64**url**, which `base64 -d` mangles |
| `fromjson` | parse the result, so you can pick fields out of it |

An empty `$TOKEN`, or `jq: error ... Malformed BOM`, means the token request failed rather than the
decode. Drop the `| jq -r .access_token` and read what Entra actually said. The usual causes are a
variable unset because you are in a different terminal, or a secret created seconds ago that has not
replicated yet - which Entra reports as `invalid_client`, indistinguishable from a wrong one.

A newly registered application gets back this:

```json
{ "aud": "30288840-e345-4543-99ee-f9253d789339", "roles": null, "azp": "<the client id>" }
```

`azp` ("authorized party") names the calling application. Its v1.0 equivalent is `appid`, which is
absent here - these are v2.0 tokens (`"ver": "2.0"`), so asking for `appid` always gives `null`.

**`aud`** is who the token is *for*. A JWT is not a general pass - it names its intended recipient,
and the slc policy rejects anything whose audience is not the marketplace API. Ask for
`scope=https://graph.microsoft.com/.default` and you get a perfectly valid token that APIM will
refuse, because it was minted for Graph.

**`roles`** is what the bearer may do there, and it is empty until the application is assigned an app
role. The same decode on the onboarding identity's own token shows what a populated one looks like:

```json
{ "aud": "https://graph.microsoft.com", "roles": ["Application.ReadWrite.OwnedBy"] }
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

## Assigning app roles, so a consumer can call an API

The credentials above authenticate but authorise nothing. What APIM's `validate-jwt` policies check
for is a **`roles` claim**, and a freshly registered application has none:

```json
{ "objectId": "...", "clientId": "...", "displayName": "colin-test", "roles": [] }
```

That empty list is why its token is rejected at the gateway. Granting a role fills it:

```json
{ "objectId": "...", "clientId": "...", "displayName": "colin-test", "roles": ["slc.read"] }
```

A `DELETE` on the same path takes it away again.

Nothing in APIM changes at any point. The policy already requires the role; the consumer's next token
simply carries it. That is the point of modelling access as app roles rather than listing client IDs
in policy XML - onboarding a consumer never touches the gateway.

The role is assigned **on the API's service principal**, naming the consumer as `principalId`:

```
POST /servicePrincipals/{api-sp}/appRoleAssignedTo
  { principalId: <consumer sp>, resourceId: <api sp>, appRoleId: <the role> }
```

Easy to get backwards - the assignment lives on the resource, not on the caller. Revoking deletes that
same assignment, which is why the read and the revoke share a lookup.

Graph answers with the role's **id**, not its name, so the names are resolved against the API's own
`appRoles`. The read needs no permission beyond what the onboarding identity already has, so it works
today even though the grant and the revoke do not.

The API is `hmcts-api-marketplace` (`ENTRA_API_CLIENT_ID`), which already defines `slc.read` and
`app.read`, both `Application` type - the right kind for `client_credentials`, where there is no user
and so no delegated scopes.

Both are **verified against the real tenant**. They need two Graph permissions, and which identity you
run as decides whether they succeed:

| Identity | `Application.ReadWrite.OwnedBy` | `AppRoleAssignment.ReadWrite.All` | |
|---|---|---|---|
| `amp-client-onboarding` | yes | **no** | registers applications, cannot assign roles |
| `amp-client-onboarding-prototype` | yes | **no** | same |
| `amp-role-assignment-test` | yes | **yes** | the default here - everything works |

Run as one of the first two and the grant answers `500` naming what is missing, which is the honest
behaviour:

```json
{ "status": 500, "title": "Internal Server Error",
  "detail": "Graph refused the app role assignment. The onboarding identity needs the
             AppRoleAssignment.ReadWrite.All application permission." }
```

`AppRoleAssignment.ReadWrite.All` is broad - it permits assigning any app role on any resource in the
tenant - which is why `apps.yaml` in `external-entra-id` deliberately scopes the onboarding identity
to owned applications only. Whether a self-serve onboarding service should hold it is a decision for
whoever owns that repo, not a technical obstacle.

## Calling the API through APIM

The point of all of it. `sps-api-mgmt-sbox` fronts the Court Schedule API at `amp/slc`:

```bash
curl -i "https://sps-api-mgmt-sbox.azure-api.net/amp/slc/case/$CASE_URN/courtschedule" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Ocp-Apim-Subscription-Key: $SUBSCRIPTION_KEY"
```

**Two credentials, doing different jobs.** The subscription key says *which subscriber this is* - it
drives the rate limit (30 calls a minute), the quota (500 a day) and the concurrency cap, all keyed on
it in the slc policy. The JWT says *this caller is genuinely who they claim and may call this API*.
Neither substitutes for the other, and `apim-subscription-key-demo` issues the first half.

The policy fails in three distinguishable ways, which is useful when something goes wrong:

| What is wrong | What comes back |
|---|---|
| No `Authorization` header | `401` with `{"error":"Missing CNP Authorization header."}` |
| Token for the wrong audience, or no `roles` | `401 Your JWT token failed CNP validation!` |
| No subscription key | `401` from APIM itself, before any policy runs |

A token with `"roles": null` fails the second of those - which is the whole reason the app role
assignment above exists.

There is also an `amp/slc-token` API on the same instance ("Get an Entra access token (sandbox)"),
which wraps the token request for anyone who would rather not assemble it by hand.

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
