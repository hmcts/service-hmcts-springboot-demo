package uk.gov.hmcts.cp.example.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class AppProperties {

    private final String caseUrnMapperBasePath;

    public AppProperties(@Value("${case-urn-mapper-client.basepath}") final String caseUrnMapperBasePath) {
        this.caseUrnMapperBasePath = caseUrnMapperBasePath;
    }
}
