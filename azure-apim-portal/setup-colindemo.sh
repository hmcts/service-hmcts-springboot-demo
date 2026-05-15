#!/bin/bash
set -e

# ── Configuration ─────────────────────────────────────────────────────────────
RESOURCE_GROUP="colindemo-rg"
APIM_NAME="colindemo"
LOCATION="uksouth"
SKU="Developer"
PUBLISHER_NAME="Colin Greenwood"
PUBLISHER_EMAIL="colingreenwood@scrumconnect.com"
API_ID="hmcts-api"
API_DISPLAY_NAME="HMCTS Hearing Results API"
API_PATH="hmcts"
PRODUCT_ID="hmcts-product"
PRODUCT_DISPLAY_NAME="HMCTS API Product"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SPEC_FILE="$SCRIPT_DIR/openapi-spec.yml"
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║  Colin Demo APIM Setup                                    ║"
echo "║  Creates APIM, imports HMCTS API, publishes dev portal    ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""
echo "  Resource group : $RESOURCE_GROUP"
echo "  APIM instance  : $APIM_NAME ($SKU, $LOCATION)"
echo "  API spec       : $SPEC_FILE"
echo "  Cost           : ~£0.07/hr while running"
echo ""
echo "  TEARDOWN when done:  az group delete --name $RESOURCE_GROUP --yes"
echo ""
read -p "Continue? (y/N) " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }

SUBSCRIPTION_ID=$(az account show --query id -o tsv)
BASE_URL="https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}"

# ── Step 1: Resource group ────────────────────────────────────────────────────
echo ""
echo "── Step 1/5: Creating resource group '$RESOURCE_GROUP'..."
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none
echo "    Done."

# ── Step 2: APIM instance ─────────────────────────────────────────────────────
echo ""
echo "── Step 2/5: Creating APIM '$APIM_NAME' — this takes 30-45 mins..."
az apim create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APIM_NAME" \
  --location "$LOCATION" \
  --sku-name "$SKU" \
  --publisher-name "$PUBLISHER_NAME" \
  --publisher-email "$PUBLISHER_EMAIL" \
  --output none
echo "    APIM ready."

# ── Step 3: Import HMCTS API ──────────────────────────────────────────────────
echo ""
echo "── Step 3/5: Importing HMCTS API from spec..."
az apim api import \
  --resource-group "$RESOURCE_GROUP" \
  --service-name "$APIM_NAME" \
  --api-id "$API_ID" \
  --path "$API_PATH" \
  --specification-format OpenApi \
  --specification-path "$SPEC_FILE" \
  --display-name "$API_DISPLAY_NAME" \
  --output none
echo "    API imported."

# ── Step 4: Product + groups ──────────────────────────────────────────────────
echo ""
echo "── Step 4/5: Setting up product and guest visibility..."
az apim product create \
  --resource-group "$RESOURCE_GROUP" \
  --service-name "$APIM_NAME" \
  --product-id "$PRODUCT_ID" \
  --product-name "$PRODUCT_DISPLAY_NAME" \
  --state published \
  --subscription-required true \
  --output none 2>/dev/null || echo "    Product already exists, skipping."

az rest --method PUT \
  --url "${BASE_URL}/products/${PRODUCT_ID}/apis/${API_ID}?api-version=2022-08-01" \
  --output none

az rest --method PUT \
  --url "${BASE_URL}/products/${PRODUCT_ID}/groups/guests?api-version=2022-08-01" \
  --output none

az rest --method PUT \
  --url "${BASE_URL}/products/${PRODUCT_ID}/groups/developers?api-version=2022-08-01" \
  --output none
echo "    Product created, API assigned, Guests + Developers groups added."

# ── Step 5: Publish Developer Portal ─────────────────────────────────────────
echo ""
echo "── Step 5/5: Publishing Developer Portal..."
echo ""
echo "    NOTE: If this fails with 'PreconditionFailed', you need to do ONE"
echo "    manual step first — open the Azure Portal, go to:"
echo "    '$APIM_NAME' → Developer Portal → Overview → Initialize developer portal"
echo "    Then re-run: sh publish-dev-portal.sh"
echo ""

az rest --method PUT \
  --url "${BASE_URL}/portalsettings/signup?api-version=2022-08-01" \
  --body '{"properties":{"enabled":true}}' \
  --output none

REVISION="r$(date +%s)"
az rest --method PUT \
  --url "${BASE_URL}/portalRevisions/${REVISION}?api-version=2022-08-01" \
  --body '{"properties":{"description":"Initial publish","isCurrent":true}}' \
  --output none

echo "    Waiting for publish to complete..."
for i in $(seq 1 30); do
  STATUS=$(az rest --method GET \
    --url "${BASE_URL}/portalRevisions/${REVISION}?api-version=2022-08-01" \
    --query "properties.status" -o tsv 2>/dev/null || echo "unknown")
  echo "    [$((i * 10))s] $STATUS"
  [[ "$STATUS" == "completed" ]] && break
  [[ "$STATUS" == "failed" ]]    && { echo "    Portal publish failed — see NOTE above."; break; }
  sleep 10
done

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║  Setup complete!                                          ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""
echo "  Gateway URL      : https://${APIM_NAME}.azure-api.net/${API_PATH}"
echo "  Developer Portal : https://${APIM_NAME}.developer.azure-api.net"
echo ""
echo "  TEARDOWN:  az group delete --name $RESOURCE_GROUP --yes"
echo ""
