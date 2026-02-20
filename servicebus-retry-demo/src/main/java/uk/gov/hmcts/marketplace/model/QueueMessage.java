package uk.gov.hmcts.marketplace.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.UUID;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessage {
    private UUID correlationId;
    private int failCount;
    private String message;

    @SneakyThrows
    public String toJson() {
        return new ObjectMapper().writeValueAsString(this);
    }

    @SneakyThrows
    public static QueueMessage fromJson(String json) {
        return new ObjectMapper().readValue(json, QueueMessage.class);
    }
}
