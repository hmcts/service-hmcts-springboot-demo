#!/bin/bash
set -e

# ── Configuration ─────────────────────────────────────────────────────────────
RESOURCE_GROUP="colindemo-rg"
APIM_NAME="colindemo"
LOCATION="uksouth"
SKU="Developer"          # Developer = cheapest tier that includes Developer Portal (~£0.07/hr)
PUBLISHER_NAME="Colin Greenwood"
PUBLISHER_EMAIL="colingreenwood@scrumconnect.com"
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "==> This will create:"
echo "    Resource group : $RESOURCE_GROUP"
echo "    APIM instance  : $APIM_NAME ($SKU SKU)"
echo "    Location       : $LOCATION"
echo ""
echo "    NOTE: Developer SKU bills at ~£0.07/hr while the instance exists."
echo "    Provisioning takes 30-45 minutes. Run the teardown at the bottom"
echo "    when you are done to stop all charges."
echo ""
read -p "Continue? (y/N) " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }

echo ""
echo "==> Creating resource group '$RESOURCE_GROUP' in $LOCATION..."
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output table

echo ""
echo "==> Creating APIM instance '$APIM_NAME' — grab a coffee, this takes 30-45 mins..."
az apim create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APIM_NAME" \
  --location "$LOCATION" \
  --sku-name "$SKU" \
  --publisher-name "$PUBLISHER_NAME" \
  --publisher-email "$PUBLISHER_EMAIL" \
  --output table

echo ""
echo "==> Done! Your APIM endpoints:"
echo ""
echo "    Gateway URL      : https://$APIM_NAME.azure-api.net"
echo "    Developer Portal : https://$APIM_NAME.developer.azure-api.net"
echo "    Management URL   : https://$APIM_NAME.management.azure-api.net"
echo ""
echo "==> Next steps:"
echo "    1. Run deploy-to-apim.sh to import the sample Products API"
echo "    2. Run publish-dev-portal.sh to publish the Developer Portal"
echo "    3. Browse to https://$APIM_NAME.developer.azure-api.net"
echo ""
echo "==> TEARDOWN (run this when done to stop all charges):"
echo "    az group delete --name $RESOURCE_GROUP --yes --no-wait"
echo ""
