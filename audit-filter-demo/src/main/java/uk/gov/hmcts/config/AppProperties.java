package uk.gov.hmcts.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Getter
@Service
@Slf4j
public class AppProperties {

    private final UUID cjscppuid;

    public AppProperties(@Value("${material-client.cjscppuid}") final UUID cjscppuid) {
        this.cjscppuid = cjscppuid;
        log.info("Initialised AppProperties with cjscppuid:{}", cjscppuid);
    }
}
