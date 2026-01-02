package uk.gov.hmcts.cp.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.model.DemoResponse;

@ExtendWith(MockitoExtension.class)
public class DemoClientTest {

    @Mock
    DemoClient demoClient;

    @Test
    void should_call_feign_client() {
        DemoResponse response = demoClient.getDemoById(123L);
    }
}
