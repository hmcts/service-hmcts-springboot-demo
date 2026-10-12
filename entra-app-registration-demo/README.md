# entra-app-registration-demo

Registers a Microsoft Entra application and hands back a **Client ID** and **Client Secret** a
subscriber can use. The backend half of the credential journey: `entra-node-login-demo` signs a
person in, this registers an application for them.

The same three Graph calls proven in the `hmcts-api-marketplace` prototype and
[service-api-marketplace#95](https://github.com/hmcts/service-api-marketplace/pull/95), through the
Microsoft Graph SDK rather than hand-written HTTP - matching `apim-subscription-key-demo`, which uses
the Azure SDK for the same reason.

## The API

| | |                                                           |
|---|---|-----------------------------------------------------------|
| `GET` | `/applications` | every application this identity owns, oldest first        |
| `GET` | `/applications/{clientId}` | get application including API roles                       |
| `POST` | `/applications/{clientId}/roles` | add role to application                                   |
| `DELETE` | `/applications/{clientId}/roles/{role}` | remove role from application                              |
| `POST` | `/applications` | register application returns the new Client ID and Secret |
| `DELETE` | `/applications/{clientId}` | remove application and credentials                        |

## The Graph calls

Registering an application is three calls, and granting it a role is two more:

```java
graph.applications().post(application)                              // create application
graph.servicePrincipals().post(servicePrincipal)                    // create sp identity
graph.applications().byApplicationId(id).addPassword().post(..)     // create client secret, returned once

graph.servicePrincipals().get(filter: "appId eq '..'")              // find the two service principals
graph.servicePrincipals().byServicePrincipalId(apiSp)
     .appRoleAssignedTo().post(assignment)                          // assign roles to the application claim
```

`com.microsoft.graph:microsoft-graph` with `ClientSecretCredential`, so the token is fetched and
refreshed by the SDK and nothing here handles one. Note `PasswordCredential.getKeyId()` returns a
`UUID`, not a `String`.

Some gotchas:

- **Everything after the first call needs retrying.** Graph's directory read replicas lag the write
  that created the application, so the next call can 404 against a stale replica - including the
  lookup that finds a service principal created seconds earlier. 20 attempts, 500ms apart, tunable
  with `ENTRA_MAX_ATTEMPTS` and `ENTRA_RETRY_DELAY_MILLIS`. Observed taking several seconds against
  the sandbox tenant, with a registration failing outright on an earlier budget of 4 x 200ms. This
  appears only against the real Graph; no mock Entra will ever show it to you.
- **Anything that fails after the application exists needs to cleanup** - including a role that cannot
be granted. Otherwise the application is left in the directory with no secret and nobody holding its
id, which is how orphans accumulate. 

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

```json
{
  "objectId": "7c1d9f2e-4b58-4a31-9f0c-2e6d8a1b3c44",
  "clientId": "e9a4b17d-6f32-4c90-8d55-1a2b3c4d5e6f",
  "clientSecret": "shown once, never again",
  "keyId": "b3f8c21a-9d47-4e65-a0b2-7c9e1f4d8a33",
  "roles": ["slc.read"]
}
```

The `/roles` endpoints exist for changing access later, which is a different event from creating the application.

Both ids come back for a reason: `clientId` is what a subscriber authenticates with, `objectId` is how
the application is addressed again afterwards. Keep it, or you cannot remove what you just made.

Listing shows what is there, oldest first, and never with a secret - Entra cannot give one back:

```json
[
  { "objectId": "...", "clientId": "...", "displayName": "an-old-test", "created": "2026-07-06T09:12:44Z" },
  { "objectId": "...", "clientId": "...", "displayName": "colin-test",  "created": "2026-10-12T08:01:02Z" }
]
```

| Setting | Default                                                                       |
|---|-------------------------------------------------------------------------------|
| `ENTRA_TENANT_ID` | the `hmctsextsbox` CIAM tenant                                                |
| `ENTRA_ONBOARDING_CLIENT_ID` | `8f867c90-...`, the amp-client-onboarding-prototype identity                  |
| `ENTRA_ONBOARDING_CLIENT_SECRET` | *required* - no default, and never will have one                              |
| `ENTRA_MAX_ATTEMPTS` | `20` - 20 x 500ms, because Entra replication really does take several seconds |
| `ENTRA_RETRY_DELAY_MILLIS` | `500`                                                                         |
| `ENTRA_GRAPH_BASE_URL` | `https://graph.microsoft.com/v1.0`                                            |
| `ENTRA_LOGIN_BASE_URL` | `https://login.microsoftonline.com`                                           |


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

Two Microsoft Graph **application** permissions:

| | |
|---|---|
| `Application.ReadWrite.OwnedBy` | register applications, add secrets, delete them - the identity owns what it creates |
| `AppRoleAssignment.ReadWrite.All` | grant a consumer a role on an API |

`amp-role-assignment-test` holds both and is the default here. `amp-client-onboarding` and
`amp-client-onboarding-prototype` hold only the first, so against those the role endpoints answer
`500` naming what is missing.

The second is broad - any app role on any resource in the tenant - which is why `apps.yaml` in
`external-entra-id` deliberately scopes the onboarding identity to owned applications only. Whether a
self-serve service should hold it is a decision for whoever owns that repo.

## What this does not do

Deliberately out of scope:

- **Persistence.** Nothing is stored. The secret is shown once and forgotten, the same behaviour
  Entra itself has.
- **Authentication in front of the API.** Anyone who can reach this service can register an
  application in the tenant.
- **Subscription keys.** APIM requires one as well as a token - `apim-subscription-key-demo` issues
  those.
