# Azure Azurite - blob storage demo

Azurite is a Docker-based emulator for Azure Storage that can be used to locally test blob operations without a real Azure account.

## This demo

A minimal Spring Boot module demonstrating 7 core blob storage operations:

| Operation | SDK call |
|-----------|----------|
| Create container | `BlobContainerClient.createIfNotExists()` |
| Upload | `BlobClient.upload(BinaryData, overwrite)` |
| Download | `BlobClient.downloadContent().toBytes()` |
| Exists check | `BlobClient.exists()` |
| Get properties | `BlobClient.getProperties()` |
| List blobs | `BlobContainerClient.listBlobs()` |
| Delete | `BlobClient.delete()` |

Integration tests spin up Azurite automatically via Testcontainers — no manual Docker setup needed.

---

## Inspecting Azurite with the `az` CLI

Start Azurite locally on port 10000:

```bash
docker run -d -p 10000:10000 mcr.microsoft.com/azure-storage/azurite:3.35.0 \
  azurite-blob --blobHost 0.0.0.0 --skipApiVersionCheck
```

All `az storage` commands below use the Azurite connection string via `--connection-string`.
Set it once for convenience:

```bash
export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;\
AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;\
BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
```

> **Note:** The `AccountKey` above is Azurite's well-known public development key — it is not a secret.

### List all containers

```bash
az storage container list --output table
```

### List blobs in a container

```bash
az storage blob list --container-name demo-container --output table
```

### Upload a blob

```bash
az storage blob upload \
  --container-name demo-container \
  --name hello.txt \
  --data "Hello from az CLI"
```

### Download a blob

```bash
az storage blob download \
  --container-name demo-container \
  --name hello.txt \
  --file /tmp/hello.txt

cat /tmp/hello.txt
```

### Get blob properties (size, content type, last modified, etc.)

```bash
az storage blob show \
  --container-name demo-container \
  --name hello.txt \
  --output json
```

### Check if a blob exists

```bash
az storage blob exists \
  --container-name demo-container \
  --name hello.txt
```

### Delete a blob

```bash
az storage blob delete \
  --container-name demo-container \
  --name hello.txt
```
