# Azure APIM Developer Portal Demo

Demonstrates how to provision Azure API Management, import an OpenAPI spec, and surface APIs through the self-hosted Developer Portal catalogue.

## Guides

| Guide | Description |
|---|---|
| [README-1-setup.md](README-1-setup.md) | Provision APIM, import APIs from a spec, publish the portal — CLI scripts walkthrough |
| [README-2-portal-guide.md](README-2-portal-guide.md) | Navigate the Azure Portal to publish, and browse the Developer Portal with screenshots |

## Files

| File | Purpose |
|---|---|
| `create-apim.sh` | Provisions the APIM instance (`colin-apim` in `colin-rg`) |
| `products-api.yaml` | Sample OpenAPI 3.0 spec — Products API with 6 operations |
| `openapi-spec.yml` | Colins API spec — Hearing Results Document Subscription |
| `deploy-to-apim.sh` | Imports the Products API and sets up a Product |
| `publish-dev-portal.sh` | Publishes the Developer Portal via the Management REST API |
| `import-api-to-apim.sh` | General-purpose script to import any API spec (file or URL) |
| `azure-screenshots/` | Screenshots used in the portal guide |
