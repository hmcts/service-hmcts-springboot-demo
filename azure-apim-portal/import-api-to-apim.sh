#!/bin/bash
set -e

# ── Configuration ─────────────────────────────────────────────────────────────
RESOURCE_GROUP="colin-rg"
APIM_NAME="colin-apim"
API_ID="colin-api"
API_DISPLAY_NAME="Colins API"
API_PATH="colin-api"
PRODUCT_ID="colin-api-product"
PRODUCT_DISPLAY_NAME="Colins APIs"
# ─────────────────────────────────────────────────────────────────────────────

usage() {
  echo "Usage: $0 --file <path-to-spec.yaml|json>"
  echo "       $0 --url  <https://yourapp.com/swagger.json>"
  exit 1
}

SPEC_FILE=""
SPEC_URL=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --file) SPEC_FILE="$2"; shift 2 ;;
    --url)  SPEC_URL="$2";  shift 2 ;;
    *) usage ;;
  esac
done

[[ -z "$SPEC_FILE" && -z "$SPEC_URL" ]] && usage
[[ -n "$SPEC_FILE" && -n "$SPEC_URL" ]] && { echo "Specify --file OR --url, not both."; exit 1; }
[[ -n "$SPEC_FILE" && ! -f "$SPEC_FILE" ]] && { echo "File not found: $SPEC_FILE"; exit 1; }

SUBSCRIPTION_ID=$(az account show --query id -o tsv)

echo ""
echo "==> Import summary:"
echo "    APIM instance  : $APIM_NAME ($RESOURCE_GROUP)"
echo "    API ID         : $API_ID"
echo "    Display name   : $API_DISPLAY_NAME"
echo "    URL path       : /$API_PATH"
echo "    Product        : $PRODUCT_DISPLAY_NAME ($PRODUCT_ID)"
[[ -n "$SPEC_FILE" ]] && echo "    Spec file      : $SPEC_FILE"
[[ -n "$SPEC_URL"  ]] && echo "    Spec URL       : $SPEC_URL"
echo ""
read -p "Proceed? (y/N) " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }

echo ""
echo "==> Verifying APIM instance..."
az apim show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APIM_NAME" \
  --query "{Name:name, SkuName:sku.name, ProvisioningState:provisioningState}" \
  -o table

echo ""
echo "==> Importing API..."
if [[ -n "$SPEC_FILE" ]]; then
  az apim api import \
    --resource-group "$RESOURCE_GROUP" \
    --service-name "$APIM_NAME" \
    --api-id "$API_ID" \
    --path "$API_PATH" \
    --specification-format OpenApi \
    --specification-path "$SPEC_FILE" \
    --display-name "$API_DISPLAY_NAME" \
    --output table
else
  az apim api import \
    --resource-group "$RESOURCE_GROUP" \
    --service-name "$APIM_NAME" \
    --api-id "$API_ID" \
    --path "$API_PATH" \
    --specification-format OpenApi \
    --specification-url "$SPEC_URL" \
    --display-name "$API_DISPLAY_NAME" \
    --output table
fi

echo ""
echo "==> Creating product '$PRODUCT_DISPLAY_NAME'..."
az apim product create \
  --resource-group "$RESOURCE_GROUP" \
  --service-name "$APIM_NAME" \
  --product-id "$PRODUCT_ID" \
  --product-name "$PRODUCT_DISPLAY_NAME" \
  --state published \
  --subscription-required true \
  --output table 2>/dev/null || echo "    Product already exists, skipping."

echo ""
echo "==> Assigning API to product..."
az rest --method PUT \
  --url "https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}/products/${PRODUCT_ID}/apis/${API_ID}?api-version=2022-08-01" \
  --output none
echo "    Done."

echo ""
echo "==> Making API visible to anonymous visitors (Guests + Developers groups)..."
az rest --method PUT \
  --url "https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}/products/${PRODUCT_ID}/groups/guests?api-version=2022-08-01" \
  --output none
az rest --method PUT \
  --url "https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}/products/${PRODUCT_ID}/groups/developers?api-version=2022-08-01" \
  --output none
echo "    Done."

echo ""
echo "==> Republishing Developer Portal..."
REVISION="r$(date +%s)"
az rest --method PUT \
  --url "https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}/portalRevisions/${REVISION}?api-version=2022-08-01" \
  --body '{"properties":{"description":"Published after API import","isCurrent":true}}' \
  --output none

echo "    Portal publish triggered (takes ~1-2 mins)."
echo ""
echo "==> All done!"
echo ""
echo "    API gateway URL : https://${APIM_NAME}.azure-api.net/${API_PATH}"
echo "    Developer Portal: https://${APIM_NAME}.developer.azure-api.net"
echo ""
