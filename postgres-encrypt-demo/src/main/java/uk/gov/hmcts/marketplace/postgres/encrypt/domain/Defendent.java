package uk.gov.hmcts.marketplace.postgres.encrypt.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.marketplace.postgres.encrypt.encryption.JsonEncryptable;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Defendent implements JsonEncryptable {
    private String name;
    private LocalDate dateOfBirth;
}
