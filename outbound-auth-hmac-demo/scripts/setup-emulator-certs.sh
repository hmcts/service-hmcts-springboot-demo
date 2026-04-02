#!/usr/bin/env bash
# Generates the self-signed SSL certificates required by the Azure Key Vault Emulator.
# Run once before starting docker-compose. Re-run to regenerate expired certs.
# Output: outbound-auth-hmac-demo/docker/certs/
#   emulator.key  – private key
#   emulator.crt  – X.509 certificate (PEM)
#   emulator.pfx  – PKCS#12 bundle (password: emulator)
#
# The certificate covers any of these hostnames:  so the emulator container can be reached by
# localhost, 127.0.0.1, keyvault-emulator (docker-compose service name), emulator

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERTS_DIR="${SCRIPT_DIR}/../docker/certs"

mkdir -p "${CERTS_DIR}"

echo "Generating emulator certificates in ${CERTS_DIR} ..."

openssl req -x509 -newkey rsa:4096 -nodes \
  -keyout "${CERTS_DIR}/emulator.key" \
  -out    "${CERTS_DIR}/emulator.crt" \
  -days   3650 \
  -subj   "/CN=keyvault-emulator" \
  -addext "subjectAltName=DNS:localhost,DNS:keyvault-emulator,DNS:emulator,IP:127.0.0.1"

openssl pkcs12 -export \
  -out    "${CERTS_DIR}/emulator.pfx" \
  -inkey  "${CERTS_DIR}/emulator.key" \
  -in     "${CERTS_DIR}/emulator.crt" \
  -passout pass:emulator

echo "Done. Files written to ${CERTS_DIR}:"
ls -1 "${CERTS_DIR}"
