package uk.gov.hmcts.audit.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import uk.gov.hmcts.audit.model.AuditMdcKeys;
import uk.gov.hmcts.audit.service.DocumentService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @InjectMocks
    private DocumentService service;

    @AfterEach
    void clearMdc() {
        MDC.remove(AuditMdcKeys.MATERIAL_ID);
    }

    @Test
    void resolving_material_id_should_put_it_in_mdc() {
        final UUID documentId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        service.resolveMaterialId(documentId);

        assertThat(MDC.get(AuditMdcKeys.MATERIAL_ID)).isNotBlank();
    }

    @Test
    void resolving_same_document_id_should_return_same_material_id() {
        final UUID documentId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        final UUID first  = service.resolveMaterialId(documentId);
        final UUID second = service.resolveMaterialId(documentId);

        assertThat(first).isEqualTo(second);
    }
}
