package uk.gov.hmcts.cp.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlobStorageService {

    private final BlobServiceClient client;

    public void createContainer(String containerName) {
        client.getBlobContainerClient(containerName).createIfNotExists();
    }

    public void upload(String containerName, String blobName, byte[] data) {
        client.getBlobContainerClient(containerName)
              .getBlobClient(blobName)
              .upload(BinaryData.fromBytes(data), true);
    }

    public byte[] download(String containerName, String blobName) {
        return client.getBlobContainerClient(containerName)
                     .getBlobClient(blobName)
                     .downloadContent()
                     .toBytes();
    }

    public boolean exists(String containerName, String blobName) {
        return client.getBlobContainerClient(containerName)
                     .getBlobClient(blobName)
                     .exists();
    }

    public BlobProperties getProperties(String containerName, String blobName) {
        return client.getBlobContainerClient(containerName)
                     .getBlobClient(blobName)
                     .getProperties();
    }

    public List<String> listBlobs(String containerName) {
        return client.getBlobContainerClient(containerName)
                     .listBlobs()
                     .stream()
                     .map(BlobItem::getName)
                     .toList();
    }

    public void delete(String containerName, String blobName) {
        client.getBlobContainerClient(containerName)
              .getBlobClient(blobName)
              .delete();
    }
}
