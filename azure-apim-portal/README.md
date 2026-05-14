# Azure APIM Developer Portal Demo

Demonstrates how to create an Azure API Management instance, import an OpenAPI spec, and surface it through the self-hosted Developer Portal catalogue.

## What this demo covers

- Provisioning an APIM instance via Azure CLI
- Importing a sample Products API from an OpenAPI 3.0 spec
- Creating a Product and assigning the API to it
- Publishing the Developer Portal so the API appears in the catalogue
- Importing your own API spec into an existing APIM instance
- Making APIs visible to anonymous (unauthenticated) visitors

---

## Files

| File | Purpose |
|---|---|
| `create-apim.sh` | Provisions the APIM instance (`colin-apim` in `colin-rg`) |
| `products-api.yaml` | Sample OpenAPI 3.0 spec — Products API with 6 operations |
| `deploy-to-apim.sh` | Imports the Products API and sets up a Product |
| `publish-dev-portal.sh` | Publishes the Developer Portal via the Management REST API |
| `import-api-to-apim.sh` | General-purpose script to import any API spec (file or URL) |

---

## Step-by-step walkthrough

### 1. Prerequisites

```bash
az login
az account set --subscription "Azure subscription 1"
```

### 2. Create the APIM instance

```bash
sh create-apim.sh
```

> This takes **30–45 minutes**. The Developer SKU (~£0.07/hr) is the cheapest tier that includes the Developer Portal. Consumption tier is free but has no portal.

### 3. Import the sample Products API

```bash
sh deploy-to-apim.sh
```

This imports `products-api.yaml`, creates a Product, assigns the API to it, and adds the Guests group so anonymous visitors can see it.

### 4. Publish the Developer Portal

```bash
sh publish-dev-portal.sh
```

A fresh APIM instance requires the portal to be initialised before the script can publish. If the script fails with `PreconditionFailed`, open the portal manually once in admin mode:

1. Go to [portal.azure.com](https://portal.azure.com) → `colin-apim`
2. Click **Developer Portal** in the left menu
3. Let it fully load (first-time initialisation)
4. Click **Publish** in the toolbar
5. Re-run `publish-dev-portal.sh` — it will work from here on

### 5. Browse the Developer Portal

```
https://colin-apim.developer.azure-api.net
```

You should see the Products API listed with all 6 operations. Click any operation to open the **Try It** console — you can make live test calls from the browser.

---

## Importing your own API

If you have an OpenAPI spec for your own service:

```bash
# From a local file
sh import-api-to-apim.sh --file ./path/to/openapi-spec.yml

# From a public URL
sh import-api-to-apim.sh --url https://yourapp.com/swagger.json
```

The script is hardcoded to `colin-api` / `Colins API` / `colin-api-product`. Edit the variables at the top of the script to change these.

### Common spec issues with APIM

APIM validates against OpenAPI **3.0**, not 3.1. Common things to fix:

| Issue | Fix |
|---|---|
| `type: [string, null]` (OpenAPI 3.1 nullable) | Change to `type: string` with `nullable: true` |
| `security` inside `components` | Move to root level |
| `--no-wait false` on `az apim create` | Omit `--no-wait` entirely |

---

## Cost and teardown

APIM Developer SKU bills continuously (~£0.07/hr) while the instance exists. There is no pause or stop option.

**To stop all charges:**

```bash
az group delete --name colin-rg --yes --no-wait
```

This deletes the resource group and everything inside it. Charges stop within ~5 minutes.

> For short demo sessions, a few hours costs pennies. A full day runs around £1.70.

---

## How the Product model works

In APIM, an **API** is the imported spec (the operations). A **Product** is the subscription wrapper around one or more APIs. Developers subscribe to a Product to get a key — the key is passed in every request as `Ocp-Apim-Subscription-Key`.

Visibility rules:
- **Guests group** → API visible without signing in
- **Developers group** → API visible to signed-in users
- No groups assigned → API hidden in the portal

The deploy and import scripts add both groups automatically.
