# apim-subscription-key-demo

Manage **Azure API Management subscription keys** from Spring Boot: create one, list them, read one, delete one.
Four endpoints, one service, the Azure SDK underneath.

It talks to the real Azure - no stand-in, no Docker - defaulted at the HMCTS sandbox instance
(`sps-api-mgmt-sbox`). It is the subscription-key half of what
[service-api-marketplace](https://github.com/hmcts/service-api-marketplace) does, on its own and small enough
to read in one sitting.

## The API

| | | |
|---|---|---|
| `POST` | `/subscription-keys` | create a subscription scoped to a product; answers with its keys |
| `GET` | `/subscription-keys` | every subscription on the instance, **without** keys |
| `GET` | `/subscription-keys/{name}` | one subscription, **with** its keys |
| `DELETE` | `/subscription-keys/{name}` | delete it, and with it the key anyone was issued |

```bash
curl -X POST localhost:8080/subscription-keys -H 'Content-Type: application/json' \
  -d '{"name":"team-alpha","productId":"cp-crime-hearing-results","displayName":"Team Alpha"}'

curl localhost:8080/subscription-keys
curl localhost:8080/subscription-keys/team-alpha
curl -X DELETE localhost:8080/subscription-keys/team-alpha
```

## Run it

You need Java 25 and to be signed in to Azure. There is no secret to set:

```bash
cd apim-subscription-key-demo

az login
export AZURE_SUBSCRIPTION_ID=$(az account show --query id -o tsv)
export AZURE_TENANT_ID=$(az account show --query tenantId -o tsv)

./gradlew bootRun
```

`DefaultAzureCredential` picks up that `az login` on a laptop and workload identity in a pod, with no code or
config difference between them - which is the whole reason to use the SDK here rather than hand-rolled HTTP.

The tenant and the Azure subscription identify a specific HMCTS tenant, so they have no value in this repo -
`application.yml` names the variables they come from and nothing else. They use the names the Azure tooling
already uses, so in AKS the workload identity webhook injects `AZURE_TENANT_ID` into the pod with nobody
setting it.

| Setting | Default | |
|---|---|---|
| `AZURE_TENANT_ID` | *required* | |
| `AZURE_SUBSCRIPTION_ID` | *required* | the **Azure** subscription the instance is billed to |
| `APIM_RESOURCE_GROUP` | `rg-sps-platform-sbox` | |
| `APIM_SERVICE_NAME` | `sps-api-mgmt-sbox` | |

If the two required variables are not set, Spring leaves the placeholder unresolved rather than refusing to
start, and the first Azure call answers `Status code 400`. An unexplained 400 here means they are missing.

## How it works

| Class | What it does |
|---|---|
| `ApimConfig` | One bean: an `ApiManagementManager` built from `DefaultAzureCredential` |
| `ApimSubscriptionKeyService` | The five SDK calls, and the mapping to something worth returning |
| `SubscriptionKeyController` | The four endpoints, and the one rule worth refusing locally: Azure's subscription-name format |

```java
apim.subscriptions().createOrUpdate(rg, service, name, parameters);
apim.subscriptions().list(rg, service);
apim.subscriptions().get(rg, service, name);
apim.subscriptions().listSecrets(rg, service, name);
apim.subscriptions().delete(rg, service, name, "*");
```

Two things in there are worth knowing before you write the same against any other API Management instance:

- **A key is always a second call.** Since api-version 2019-01-01 Azure leaves `primaryKey` and `secondaryKey`
  out of both `get` and `list`, so `create` and `get` each follow up with `listSecrets`. That is why listing can
  be done by something not allowed to see any key at all - and it is the hinge the least privilege below turns on.
- **`delete` takes an `If-Match`.** Azure rejects an APIM entity delete without one; the SDK makes it an argument
  you cannot leave out, so `"*"` ("whatever version is there now") is explicit rather than forgotten.

Also worth knowing: `regeneratePrimaryKey` / `regenerateSecondaryKey` are right there on the same interface when
rotation is wanted. This demo does not use them.

### Why the SDK and not raw HTTP

Both work - the SDK calls the same ARM REST API this would otherwise be hand-written against. The SDK wins on
the things that are tedious and easy to get wrong:

| | Raw HTTP | SDK |
|---|---|---|
| Credential | Hand-rolled client-credentials call; workload identity means implementing the federated client assertion yourself | `DefaultAzureCredential`, one line, same code locally and in a pod |
| Token lifetime | Cache it yourself or fetch one per call | Cached and refreshed by the pipeline |
| ARM throttling | ARM returns 429 with `Retry-After`; handle it yourself | Retry policy in the pipeline |
| `If-Match` on delete | A header you can forget | A parameter you cannot |
| Models | Hand-written records mirroring ARM's JSON | Typed, and they track the api-version |

The cost is a sizeable dependency tree (`azure-core`, reactor-netty) and a library that trails ARM's newest
api-versions. For subscription keys, neither matters. It also matches the rest of this repo - `azure-vault-demo`,
`azure-appconfig-demo` and the `servicebus-*` demos all use the SDK with `DefaultAzureCredential`.

## Testing it against the real thing

There is no APIM emulator to test against. Microsoft ships emulators for data planes only - Azurite for Storage,
`azure-messaging/servicebus-emulator` for Service Bus - and nothing at all for `management.azure.com`. The one
APIM container that exists, the self-hosted gateway, is a data-plane component that will not even start without
a real APIM instance to pull its configuration from. So the only thing that proves Azure accepts these calls is
Azure.

`ApimSubscriptionKeyFunctionalTest` does that against the sandbox instance. It is `@EnabledOnOs(OS.MAC)`, so it
runs on a developer's machine and skips on the `ubuntu-latest` runners; `unit-test.gradle` also excludes it from
`test`, so the pipeline never reaches it either way. Sign in and run its own task:

```bash
cd apim-subscription-key-demo

az login
export AZURE_SUBSCRIPTION_ID=$(az account show --query id -o tsv)
export AZURE_TENANT_ID=$(az account show --query tenantId -o tsv)

./gradlew functionalTest
```

Two of its three tests only read. The third creates a uniquely named subscription, reads its key back and deletes
it again, so it does write to the instance - briefly, and under a `functest-` name that identifies where it came
from.

Between them they prove what the unit tests cannot: that `DefaultAzureCredential` finds a credential, that the
tenant, subscription, resource group and service name point at something real, that the credential has
`subscriptions/read` and `subscriptions/listSecrets/action` - a separate permission from the read, and the one
most likely to have been missed - and that `delete` is sending the `If-Match` Azure insists on.

## What Azure permissions the service needs

All one data plane - ARM - so it is all Azure RBAC on the APIM instance. No Microsoft Graph and no Entra app
roles, which is the main way this differs from issuing client IDs and secrets.

### The actions

| Action | Needed for |
|---|---|
| `Microsoft.ApiManagement/service/subscriptions/read` | list, get |
| `Microsoft.ApiManagement/service/subscriptions/write` | create |
| `Microsoft.ApiManagement/service/subscriptions/delete` | delete |
| `Microsoft.ApiManagement/service/subscriptions/listSecrets/action` | the keys themselves |
| `Microsoft.ApiManagement/service/read` | reading the instance the rest hang off |
| `Microsoft.ApiManagement/service/products/read` | only if you want to check a product exists before scoping to it |

### Why not a built-in role

| Built-in role | Why not |
|---|---|
| **API Management Service Contributor** | Works, and grants far too much - it can also change APIs, policies, products and the instance itself |
| **API Management Service Reader Role** | Read-only: cannot create or delete, and `listSecrets` is a write-shaped action it does not carry |
| **API Management Service Operator Role** | Manages the instance (backups, scaling) but not its entities, so it cannot touch subscriptions at all |

So: a custom role with exactly the six actions above, assigned at the scope of the one APIM instance - not the
resource group, and certainly not the subscription.

### Terraform

Three pieces: a custom role, an identity, and the federation that lets a pod be that identity without a secret.

```hcl
data "azurerm_api_management" "apim" {
  name                = "sps-api-mgmt-sbox"
  resource_group_name = "rg-sps-platform-sbox"
}

resource "azurerm_role_definition" "apim_subscription_keys" {
  name        = "APIM Subscription Key Manager"
  scope       = data.azurerm_api_management.apim.id
  description = "Create, read, list and delete APIM subscriptions and their keys. Nothing else."

  permissions {
    actions = [
      "Microsoft.ApiManagement/service/read",
      "Microsoft.ApiManagement/service/products/read",
      "Microsoft.ApiManagement/service/subscriptions/read",
      "Microsoft.ApiManagement/service/subscriptions/write",
      "Microsoft.ApiManagement/service/subscriptions/delete",
      "Microsoft.ApiManagement/service/subscriptions/listSecrets/action",
    ]
    not_actions = []
  }

  assignable_scopes = [data.azurerm_api_management.apim.id]
}

resource "azurerm_user_assigned_identity" "apim_keys" {
  name                = "id-apim-subscription-keys-${var.env}"
  resource_group_name = var.resource_group_name
  location            = var.location
}

resource "azurerm_role_assignment" "apim_keys" {
  scope              = data.azurerm_api_management.apim.id
  role_definition_id = azurerm_role_definition.apim_subscription_keys.role_definition_resource_id
  principal_id       = azurerm_user_assigned_identity.apim_keys.principal_id
}

resource "azurerm_federated_identity_credential" "aks" {
  name                = "apim-subscription-keys"
  resource_group_name = var.resource_group_name
  parent_id           = azurerm_user_assigned_identity.apim_keys.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = data.azurerm_kubernetes_cluster.aks.oidc_issuer_url
  subject             = "system:serviceaccount:${var.namespace}:${var.service_account_name}"
}
```

### In the cluster

The service account carries the identity's client ID, and the pod opts in:

```yaml
serviceAccount:
  annotations:
    azure.workload.identity/client-id: <azurerm_user_assigned_identity.apim_keys.client_id>
podLabels:
  azure.workload.identity/use: "true"
```

The AKS webhook then injects `AZURE_CLIENT_ID`, `AZURE_TENANT_ID` and `AZURE_FEDERATED_TOKEN_FILE`, and
`DefaultAzureCredential` uses them. No Key Vault entry, no secret to rotate, and no code change from the laptop
version - the subject in the federated credential is the only thing tying them together, so it must match the
namespace and service account the pod actually runs as.

One thing terraform cannot do for you: creating the custom role and assigning it both need
`Microsoft.Authorization/roleDefinitions/write` and `…/roleAssignments/write` at the APIM scope, which the
pipeline's own service principal must already have been granted (Owner or User Access Administrator there). That
grant is the usual place this gets stuck.

## What is not here

Deliberately: no persistence, no authentication in front of the API, and no key rotation. The point is the shape
of the Azure calls and the permissions they need.
