package uk.gov.hmcts.cp.example.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class AppProperties {

    private final String caseMapperUrl;
    private final String caseMapperPath;

    public AppProperties(
            @Value("${case-mapper-client.url}") final String caseMapperUrl,
            @Value("${case-mapper-client.path}") final String caseMapperPath) {
        this.caseMapperUrl = caseMapperUrl;
        this.caseMapperPath = caseMapperPath;
    }
}
