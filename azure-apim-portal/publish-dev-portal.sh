#!/bin/bash
set -e

# ── Configuration ─────────────────────────────────────────────────────────────
RESOURCE_GROUP="colindemo-rg"
APIM_NAME="colindemo"
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "==> Getting subscription ID..."
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
echo "    Subscription: $SUBSCRIPTION_ID"

BASE_URL="https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.ApiManagement/service/${APIM_NAME}"

echo ""
echo "==> Enabling Developer Portal sign-up..."
az rest --method PUT \
  --url "${BASE_URL}/portalsettings/signup?api-version=2022-08-01" \
  --body '{"properties":{"enabled":true}}' \
  --output none
echo "    Sign-up enabled."

echo ""
echo "==> Creating portal revision and publishing..."
REVISION="r$(date +%s)"

az rest --method PUT \
  --url "${BASE_URL}/portalRevisions/${REVISION}?api-version=2022-08-01" \
  --body '{"properties":{"description":"Published via script","isCurrent":true}}' \
  --output none

echo "    Revision $REVISION created. Waiting for publish to complete..."
echo ""

for i in $(seq 1 24); do
  STATUS=$(az rest --method GET \
    --url "${BASE_URL}/portalRevisions/${REVISION}?api-version=2022-08-01" \
    --query "properties.status" -o tsv 2>/dev/null || echo "unknown")

  echo "    [$i/24] Status: $STATUS"

  if [[ "$STATUS" == "completed" ]]; then
    echo ""
    echo "==> Developer Portal published successfully!"
    echo ""
    echo "    Browse to: https://${APIM_NAME}.developer.azure-api.net"
    echo ""
    exit 0
  fi

  if [[ "$STATUS" == "failed" ]]; then
    echo ""
    echo "    Publish failed. Try publishing manually:"
    echo "    portal.azure.com -> $APIM_NAME -> Developer Portal -> Publish"
    exit 1
  fi

  sleep 10
done

echo ""
echo "    Timed out waiting — the portal is likely still publishing."
echo "    Check: https://${APIM_NAME}.developer.azure-api.net in a minute or two."
echo ""
echo "    If it still doesn't appear, publish manually:"
echo "    portal.azure.com -> $APIM_NAME -> Developer Portal -> Publish"
echo ""
