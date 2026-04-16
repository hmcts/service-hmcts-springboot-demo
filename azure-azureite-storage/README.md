# Azure azureite is a docker emulator that can be used to locally test Azure file storage

# An example using TestContainers to spin up azurite docker container in integration tests can be found at
https://github.com/hmcts/cp-case-document-knowledge-service

https://github.com/hmcts/cp-court-list-publishing-service

# This demo provides a cut down simple implementation

We focus on showing 7 common storage operations
createIfNotExists()
BlobClient.upload(BinaryData, overwrite)
BlobClient.downloadContent().toBytes()
BlobClient.exists()
BlobClient.getProperties()
BlobContainerClient.listBlobs()
BlobClient.delete()

# We can also use the "az" cli to see whats happening in the azurite emulator