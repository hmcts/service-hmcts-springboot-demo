package uk.gov.hmcts.marketplace.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicAdminServiceTest {

    @Mock
    ServiceBusConfigService configService;

    @InjectMocks
    TopicAdminService topicAdminService;

    @Mock
    ServiceBusAdministrationClient administrationClient;
    @Mock
    PagedIterable<TopicProperties> topics;

    @Test
    void service_bus_started_should_return_true() {
        when(configService.adminClient()).thenReturn(administrationClient);
        when(administrationClient.listTopics()).thenReturn(topics);

        assertThat(topicAdminService.isServiceBusReady()).isTrue();
    }

    @Test
    void service_bus_down_should_throw() {
        when(configService.adminClient()).thenThrow(new RuntimeException("Connection refused"));
        assertThat(topicAdminService.isServiceBusReady()).isFalse();
    }
}