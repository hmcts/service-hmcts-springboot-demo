#!/usr/bin/env bash
# Queries Azure App Configuration feature flags via the Azure CLI.
# Prerequisites: az login && az account set --subscription <id>
set -euo pipefail

APP_CONFIG_NAME="${APP_CONFIG_NAME:-NLE-CCP01-APPCONFIG}"
LABEL="${LABEL:-STE30}"
FEATURE="${FEATURE:-hearingResultsDocumentSubscriptionEnabled}"

echo "=== Azure App Config Demo ==="
echo "Store : $APP_CONFIG_NAME"
echo "Label : $LABEL"
echo "Feature: $FEATURE"
echo ""

echo "--- Feature flag detail ---"
az appconfig feature show \
  --name "$APP_CONFIG_NAME" \
  --feature "$FEATURE" \
  --label "$LABEL"

echo ""
echo "--- Enabled? ---"
ENABLED=$(az appconfig feature show \
  --name "$APP_CONFIG_NAME" \
  --feature "$FEATURE" \
  --label "$LABEL" \
  --query "enabled" \
  --output tsv)

echo "$FEATURE is enabled: $ENABLED"

echo ""
echo "--- All feature flags for label $LABEL ---"
az appconfig feature list \
  --name "$APP_CONFIG_NAME" \
  --label "$LABEL" \
  --query "[].{name:name, enabled:enabled}" \
  --output table
