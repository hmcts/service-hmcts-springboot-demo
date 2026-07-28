package uk.gov.hmcts.marketplace.postgres.encrypt.encryption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA converter that serialises any {@link JsonEncryptable} to JSON, encrypts it,
 * and stores it as a single text column. The class name is embedded in the payload
 * so the correct type is restored on read — only {@link JsonEncryptable} implementors
 * are accepted, preventing arbitrary class deserialisation.
 */
@Converter(autoApply = true)
@Component
@AllArgsConstructor
public class JsonEncryptableConverter implements AttributeConverter<JsonEncryptable, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(JsonEncryptable value) {
        if (value == null) {
            return null;
        }
        try {
            ObjectNode wrapper = OBJECT_MAPPER.createObjectNode()
                    .put("type", value.getClass().getName());
            wrapper.set("data", OBJECT_MAPPER.valueToTree(value));
            return encryptionService.encrypt(OBJECT_MAPPER.writeValueAsString(wrapper));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise " + value.getClass().getSimpleName() + " to JSON", e);
        }
    }

    @Override
    public JsonEncryptable convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            String json = encryptionService.decrypt(dbData);
            JsonNode root = OBJECT_MAPPER.readTree(json);
            String typeName = root.get("type").asText();
            Class<?> clazz = Class.forName(typeName);
            if (!JsonEncryptable.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Refusing to deserialise non-JsonEncryptable type: " + typeName);
            }
            return (JsonEncryptable) OBJECT_MAPPER.treeToValue(root.get("data"), clazz);
        } catch (JsonProcessingException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialise JsonEncryptable from database", e);
        }
    }
}
