package uk.gov.hmcts.marketplace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

@ExtendWith(MockitoExtension.class)
class TopicAdminServiceTest {

    @Mock
    ServiceBusConfigService configService;

    @InjectMocks
    TopicAdminService topicAdminService;

    @Test
    void admin_service_should_detect_started() {
        // when(configService).thenReturn()
        topicAdminService.isServiceBusReady();
    }
}