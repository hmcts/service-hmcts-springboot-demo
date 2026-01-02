package uk.gov.hmcts.cp.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.cp.model.DemoResponse;

@FeignClient(
        name = "demo-service",
        url = "${demo-service.url}"
)
public interface DemoClient {

    @GetMapping("/api/demo/{id}")
    DemoResponse getDemoById(@PathVariable Long id);
}
