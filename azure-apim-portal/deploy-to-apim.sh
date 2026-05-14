#!/bin/bash
set -e

# ── Configuration ─────────────────────────────────────────────────────────────
RESOURCE_GROUP="colin-rg"
APIM_NAME="colin-apim"
API_ID="products-api"
API_DISPLAY_NAME="Products API"
API_PATH="products"
PRODUCT_ID="products"
PRODUCT_DISPLAY_NAME="Products"
SPEC_FILE="$(dirname "$0")/products-api.yaml"
# ─────────────────────────────────────────────────────────────────────────────

SUBSCRIPTION_ID=$(az account show --query id -o tsv)

echo ""
echo "==> Current Azure subscription:"
az account show --query "{Name:name, SubscriptionId:id}" -o table

echo ""
echo "==> Verifying APIM instance exists..."
az apim show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APIM_NAME" \
  --query "{Name:name, SkuName:sku.name, ProvisioningState:provisioningState}" \
  -o table

echo ""
echo "==> Importing Products API from OpenAPI spec..."
az apim api import \
  --resource-group "$RESOURCE_GROUP" \
  --service-name "$APIM_NAME" \
  --api-id "$API_ID" \
  --path "$API_PATH" \
  --specification-format OpenApi \
  --specification-path "$SPEC_FILE" \
  --display-name "$API_DISPLAY_NAME" \
  --output table

echo ""
echo "==> Creating Product '$PRODUCT_DISPLAY_NAME'..."
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
echo "==> Adding Guests and Developers groups to product (makes API visible publicly)..."
az rest --method PUT \
  --url "https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}/products/${PRODUCT_ID}/groups/guests?api-version=2022-08-01" \
  --output none
az rest --method PUT \
  --url "https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}/products/${PRODUCT_ID}/groups/developers?api-version=2022-08-01" \
  --output none
echo "    Done."

echo ""
echo "==> Products API imported successfully!"
echo ""
echo "    Next: run publish-dev-portal.sh to make it visible in the Developer Portal."
echo ""
