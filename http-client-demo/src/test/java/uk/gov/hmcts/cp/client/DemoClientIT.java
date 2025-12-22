package uk.gov.hmcts.cp.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import uk.gov.hmcts.cp.model.DemoResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@EnableWireMock({
        @ConfigureWireMock(name = "demo-service", port = 0, filesUnderClasspath = "stubs",
                baseUrlProperties = "demo-service.url")
})
class DemoClientIT {

    @Autowired
    private DemoClient demoClient;

    @Test
    void shouldReturnDemoById() {

        DemoResponse response = demoClient.getDemoById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Demo Name");
        assertThat(response.getEmail()).isEqualTo("demo@email.com");
    }

    @Test
    void shouldThrowNotFoundWhenDemoDoesNotExist() {
        assertThatThrownBy(() -> demoClient.getDemoById(999L))
                .isInstanceOf(feign.FeignException.NotFound.class);
    }
}
