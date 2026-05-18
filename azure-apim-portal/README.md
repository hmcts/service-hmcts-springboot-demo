# Azure APIM Developer Portal Demo

Demonstrates how to provision Azure API Management, import an OpenAPI spec, and surface APIs through the hosted Developer Portal with mock responses.

## Guides

| Guide | Description |
|---|---|
| [README-1-setup.md](README-1-setup.md) | Provision APIM, import APIs from a spec, publish the portal — CLI scripts walkthrough |
| [README-2-portal-guide.md](README-2-portal-guide.md) | Navigate the Azure Portal to publish, and browse the Developer Portal with screenshots |

## Files

| File | Purpose |
|---|---|
| `setup-colindemo.sh` | **End-to-end setup** — creates RG, APIM, imports HMCTS spec, publishes portal |
| `create-apim.sh` | Provisions the APIM instance |
| `openapi-spec.yml` | HMCTS API spec — Hearing Results Document Subscription |
| `import-api-to-apim.sh` | Imports/re-imports any API spec into an existing APIM instance |
| `publish-dev-portal.sh` | Re-publishes the Developer Portal via the Management REST API |
| `home-page-snippet.html` | Styled home page design (see [Home page](#home-page) below) |
| `products-api.yaml` | Sample OpenAPI 3.0 spec — Products API with 6 operations |
| `deploy-to-apim.sh` | Imports the Products API and sets up a Product |
| `azure-screenshots/` | Screenshots used in the portal guide |

---

## Quick start

```bash
sh azure-apim-portal/setup-colindemo.sh
```

| Endpoint | URL |
|---|---|
| Gateway | `https://colindemo.azure-api.net/hmcts` |
| Developer Portal | `https://colindemo.developer.azure-api.net` |

**Teardown:** `az group delete --name colindemo-rg --yes`

---

## Mock responses

There is no live backend — all operations return example data from the spec via
an APIM inbound policy, so both `curl` and the portal's **Try this operation**
console work out of the box.

| Operation | Method | Path | Mock status |
|---|---|---|---|
| `GetEventTypes` | GET | `/event-types` | 200 – event types list |
| `createClientSubscription` | POST | `/client-subscriptions` | 201 – subscription + HMAC key |
| `getClientSubscription` | GET | `/client-subscriptions/{id}` | 200 – subscription details |
| `updateClientSubscription` | PUT | `/client-subscriptions/{id}` | 200 – updated subscription |
| `deleteClientSubscription` | DELETE | `/client-subscriptions/{id}` | 204 – no content |
| `createNotification` | POST | `/notifications` | 202 – accepted |
| `rotateClientSubscriptionSecret` | POST | `/client-subscriptions/{id}/secret/rotate` | 200 – new HMAC secret |
| `getDocument` | GET | `/client-subscriptions/{id}/documents/{id}` | 200 – PDF |

The policy uses `return-response` per operation in the API inbound section:

```xml
<policies>
  <inbound>
    <choose>
      <when condition="@(context.Operation.Id == &quot;GetEventTypes&quot;)">
        <return-response>
          <set-status code="200" reason="OK" />
          <set-header name="Access-Control-Allow-Origin" exists-action="override">
            <value>https://colindemo.developer.azure-api.net</value>
          </set-header>
          <set-header name="Content-Type" exists-action="override">
            <value>application/json</value>
          </set-header>
          <set-body>{"eventTypes":[{"eventName":"WEE_CustodialSentence",...}]}</set-body>
        </return-response>
      </when>
      <!-- one <when> per operation -->
    </choose>
    <base />
  </inbound>
  ...
</policies>
```

---

## "Try this operation" — CORS fix

The Developer Portal **Try this operation** console runs in the browser and makes
a cross-origin request from `*.developer.azure-api.net` to `*.azure-api.net`.
Two things must be in place:

### 1. Global CORS policy (handles OPTIONS preflight)

```xml
<cors allow-credentials="false">
  <allowed-origins>
    <origin>https://colindemo.developer.azure-api.net</origin>
  </allowed-origins>
  <allowed-methods preflight-result-max-age="300">
    <method>GET</method><method>POST</method><method>PUT</method>
    <method>PATCH</method><method>DELETE</method><method>OPTIONS</method>
  </allowed-methods>
  <allowed-headers><header>*</header></allowed-headers>
</cors>
```

### 2. `Access-Control-Allow-Origin` inside every `return-response`

`return-response` short-circuits the pipeline and bypasses the outbound phase,
so the CORS policy never gets a chance to add its headers to the response.
The header **must** be set explicitly inside each `return-response` block:

```xml
<set-header name="Access-Control-Allow-Origin" exists-action="override">
  <value>https://colindemo.developer.azure-api.net</value>
</set-header>
```

> **Without this**, the gateway returns `200 OK` with the correct JSON body but
> the browser silently drops the response — shown as **503** in the portal UI.

### 3. `servers` URL in the OpenAPI spec

The spec's `servers` field must be the APIM gateway URL (not a placeholder):

```yaml
servers:
  - url: https://colindemo.azure-api.net/hmcts
```

If this points to `https://api.example.com`, the portal's Try it console calls
the placeholder URL directly and gets 503 with no policy interception.

---

## Home page

The portal home page has three navigation cards deployed using native APIM
portal widgets (Browse APIs, Get a Subscription Key, HMCTS GitHub).

For the **fully-styled version** (icon rows with hover effects, as in
`home-page-snippet.html`), use the Azure Portal visual editor — the portal's
blob storage for custom HTML widgets is not accessible via the ARM REST API:

1. `portal.azure.com` → `colindemo` → **Developer portal** → open editor
2. Delete the current page content
3. Drag in a **Custom HTML code** widget
4. Paste the contents of `home-page-snippet.html`
5. Click **Publish**
