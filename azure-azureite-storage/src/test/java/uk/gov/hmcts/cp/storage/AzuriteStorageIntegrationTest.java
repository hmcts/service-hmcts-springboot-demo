package uk.gov.hmcts.cp.storage;

import com.azure.storage.blob.models.BlobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AzuriteContainerInitialise.class)
@ContextConfiguration(initializers = AzuriteContainerInitialise.class)
class AzuriteStorageIntegrationTest {

    private static final String CONTAINER = "demo-container";

    @Autowired
    BlobStorageService blobStorageService;

    @BeforeEach
    void setUp() {
        blobStorageService.createContainer(CONTAINER);
    }

    @Test
    void upload_blob_should_be_downloadable() {
        byte[] content = "Hello, Azurite!".getBytes(StandardCharsets.UTF_8);

        blobStorageService.upload(CONTAINER, "greeting.txt", content);

        assertThat(blobStorageService.download(CONTAINER, "greeting.txt")).isEqualTo(content);
    }

    @Test
    void upload_blob_should_change_exists_from_false_to_true() {
        assertThat(blobStorageService.exists(CONTAINER, "file.txt")).isFalse();

        blobStorageService.upload(CONTAINER, "file.txt", "data".getBytes(StandardCharsets.UTF_8));

        assertThat(blobStorageService.exists(CONTAINER, "file.txt")).isTrue();
    }

    @Test
    void upload_blob_should_report_correct_size_in_properties() {
        byte[] content = "size-check".getBytes(StandardCharsets.UTF_8);
        blobStorageService.upload(CONTAINER, "sized.txt", content);

        BlobProperties props = blobStorageService.getProperties(CONTAINER, "sized.txt");

        assertThat(props.getBlobSize()).isEqualTo(content.length);
    }

    @Test
    void upload_blobs_should_all_appear_in_list() {
        // use a dedicated container so this test is not affected by blobs from other tests
        String listContainer = "list-test-container";
        blobStorageService.createContainer(listContainer);

        blobStorageService.upload(listContainer, "alpha.txt", "a".getBytes(StandardCharsets.UTF_8));
        blobStorageService.upload(listContainer, "beta.txt", "b".getBytes(StandardCharsets.UTF_8));

        assertThat(blobStorageService.listBlobs(listContainer))
                .containsExactlyInAnyOrder("alpha.txt", "beta.txt");
    }

    @Test
    void delete_blob_should_no_longer_exist() {
        blobStorageService.upload(CONTAINER, "temp.txt", "temp".getBytes(StandardCharsets.UTF_8));
        assertThat(blobStorageService.exists(CONTAINER, "temp.txt")).isTrue();

        blobStorageService.delete(CONTAINER, "temp.txt");

        assertThat(blobStorageService.exists(CONTAINER, "temp.txt")).isFalse();
    }
}
