# Azure APIM — Setup & API Import Guide

Step-by-step guide to provisioning an Azure API Management instance, importing an API from an OpenAPI spec, and publishing the Developer Portal.

---

## Prerequisites

```bash
az login
az account set --subscription "Azure subscription 1"
```

---

## Step 1 — Create the APIM instance

```bash
sh create-apim.sh
```

This provisions `colin-apim` in resource group `colin-rg` using the **Developer SKU** in `uksouth`.

> **Why Developer SKU?** It is the cheapest tier (~£0.07/hr) that includes the Developer Portal. The Consumption tier is free per-call but has no portal.

**This takes 30–45 minutes.** Azure is standing up a lot of infrastructure under the hood — the wait is unavoidable regardless of how you trigger it (CLI, ARM template, Bicep, or Terraform).

Once done, the script prints your endpoints:

```
Gateway URL      : https://colin-apim.azure-api.net
Developer Portal : https://colin-apim.developer.azure-api.net
```

---

## Step 2 — Import the sample Products API

```bash
sh deploy-to-apim.sh
```

This:
- Imports `products-api.yaml` (OpenAPI 3.0, 6 operations)
- Creates a **Product** called `Products` and assigns the API to it
- Adds the **Guests** and **Developers** groups so the API is visible to anonymous visitors

---

## Step 3 — Import your own API

If you have your own OpenAPI spec:

```bash
# From a local file
sh import-api-to-apim.sh --file ./openapi-spec.yml

# From a public URL
sh import-api-to-apim.sh --url https://yourapp.com/swagger.json
```

The script is hardcoded to use `colin-api` / `Colins API` / `colin-api-product`. Edit the variables at the top to change these.

---

## Step 4 — Publish the Developer Portal

```bash
sh publish-dev-portal.sh
```

The script uses the Azure Management REST API to trigger a portal publish and polls until it completes (~1–2 minutes).

**First-time only:** A brand new APIM instance requires the portal to be initialised in a browser before the script can publish. If you see a `PreconditionFailed` error:

1. Go to [portal.azure.com](https://portal.azure.com) → `colin-apim`
2. Click **Developer Portal** → **Portal overview** in the left menu
3. Let it fully load in the browser (this initialises the portal content for the first time)
4. Re-run `sh publish-dev-portal.sh` — it will work from here on

See [README-2-portal-guide.md](README-2-portal-guide.md) for screenshots of the manual publish flow.

---

## Common spec validation errors

APIM validates against OpenAPI **3.0**, not 3.1. Common issues:

| Error | Cause | Fix |
|---|---|---|
| `Cannot create a scalar value from this type of node` | `type: [string, null]` — OpenAPI 3.1 nullable syntax | Change to `type: string` + `nullable: true` |
| `security is not a valid property at #/components` | `security` placed inside `components` | Move `security` to root level |
| `az apim create` fails immediately | `--no-wait false` or `--enable-client-certificate false` passed | Remove both flags — they are not valid parameters |

---

## Cost and teardown

APIM Developer SKU bills continuously (~£0.07/hr) while the instance exists. There is **no pause or stop option** — unlike VMs, you cannot deallocate it.

| Usage | Approx. cost |
|---|---|
| 2–3 hours playing | <£0.25 |
| Full day | ~£1.70 |
| Full week | ~£12 |

**To delete everything and stop all charges:**

```bash
az group delete --name colin-rg --yes --no-wait
```

Charges stop within ~5 minutes of deletion.

---

## How the Product model works

In APIM, an **API** is the imported spec (the gateway endpoints). A **Product** is the subscription wrapper — developers subscribe to a Product to receive a key, which they pass with every request as `Ocp-Apim-Subscription-Key`.

Visibility is controlled by groups assigned to the product:

| Group | Effect |
|---|---|
| **Guests** | API visible to anonymous (unauthenticated) visitors |
| **Developers** | API visible to signed-in portal users |
| *(none)* | API hidden from the portal |

The deploy and import scripts add both groups automatically.
