package uk.gov.hmcts.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.hmcts.audit.annotation.AuditDetail;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.audit.model.AuditEventType;
import uk.gov.hmcts.audit.model.AuditMessage;
import uk.gov.hmcts.audit.model.AuditMetadata;
import uk.gov.hmcts.audit.model.AuditPayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.audit.filter.AuditFilter.CORRELATION_ID_HEADER;

@ExtendWith(MockitoExtension.class)
class AuditPayloadGenerationServiceTest {

    @Mock private HandlerMethod handlerMethod;
    @Mock private HttpServletRequest request;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private AuditPayloadGenerationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "systemUserId", "31ec3a16-8721-498c-8da5-f099390ee254");
    }

    @Test
    void generating_payload_should_produce_spec_compliant_envelope_with_origin_component_timestamp_and_content() throws Exception {
        final AuditDetail detail = auditDetail("hearing-results-document", "QUERY_API", "hearing-results-document.get-document", "Download");
        when(handlerMethod.getMethodAnnotation(AuditDetail.class)).thenReturn(detail);
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("corr-abc-123");
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of(
                        "clientSubscriptionId", "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                        "documentId",           "7c9e6679-7425-40de-944b-e07fc1f90ae7"));

        final AuditMessage payload = objectMapper.readValue(
                service.generate(handlerMethod, request, AuditEventType.REQUEST, null), AuditMessage.class);

        assertThat(payload.origin()).isEqualTo("hearing-results-document");
        assertThat(payload.component()).isEqualTo("QUERY_API");
        assertThat(payload.timestamp()).isNotBlank();
        assertThat(payload.content()).isNotNull();
    }

    @Test
    void generating_payload_content_should_include_metadata_action_correlation_id_and_path_params() throws Exception {
        final UUID subId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        final UUID docId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
        final AuditDetail detail = auditDetail("hearing-results-document", "QUERY_API", "hearing-results-document.get-document", "Download");
        when(handlerMethod.getMethodAnnotation(AuditDetail.class)).thenReturn(detail);
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("corr-abc-123");
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("clientSubscriptionId", subId.toString(), "documentId", docId.toString()));

        final AuditMessage payload = objectMapper.readValue(
                service.generate(handlerMethod, request, AuditEventType.RESPONSE, 200), AuditMessage.class);

        final AuditPayload content = payload.content();
        assertThat(content.eventType()).isEqualTo(AuditEventType.RESPONSE);
        assertThat(content.responseStatus()).isEqualTo(200);
        assertThat(content.action()).isEqualTo("Download");
        assertThat(content.correlationId()).isEqualTo("corr-abc-123");
        assertThat(content.pathParams()).containsEntry("clientSubscriptionId", subId);
        assertThat(content.pathParams()).containsEntry("documentId", docId);

        final AuditMetadata metadata = content.metadata();
        assertThat(metadata.id()).isNotNull();
        assertThat(metadata.name()).isEqualTo("hearing-results-document.get-document");
        assertThat(metadata.context().user()).isEqualTo("31ec3a16-8721-498c-8da5-f099390ee254");
    }

    @Test
    void generating_payload_with_no_annotation_should_return_null() {
        when(handlerMethod.getMethodAnnotation(AuditDetail.class)).thenReturn(null);
        when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);

        assertThat(service.generate(handlerMethod, request, AuditEventType.REQUEST, null)).isNull();
    }

    @Test
    void generating_payload_should_only_include_declared_path_params() throws Exception {
        final AuditDetail detail = auditDetail("my-service", "COMMAND_API", "my-service.get-item", "View");
        when(handlerMethod.getMethodAnnotation(AuditDetail.class)).thenReturn(detail);
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("corr-xyz");
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of(
                        "clientSubscriptionId", "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                        "documentId",           "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                        "undeclared",           "00000000-0000-0000-0000-000000000000"));

        final AuditMessage payload = objectMapper.readValue(
                service.generate(handlerMethod, request, AuditEventType.REQUEST, null), AuditMessage.class);

        assertThat(payload.content().eventType()).isEqualTo(AuditEventType.REQUEST);
        assertThat(payload.content().responseStatus()).isNull();
        assertThat(payload.content().pathParams()).containsOnlyKeys("clientSubscriptionId", "documentId");
        assertThat(payload.content().pathParams()).doesNotContainKey("undeclared");
    }

    private AuditDetail auditDetail(final String origin, final String component, final String eventName, final String action) {
        final AuditDetail detail = mock(AuditDetail.class);
        when(detail.origin()).thenReturn(origin);
        when(detail.component()).thenReturn(component);
        when(detail.eventName()).thenReturn(eventName);
        when(detail.action()).thenReturn(action);
        when(detail.pathParams()).thenReturn(new String[]{"clientSubscriptionId", "documentId"});
        return detail;
    }
}
