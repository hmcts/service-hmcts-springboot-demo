package uk.gov.hmcts.marketplace.postgres.encrypt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.marketplace.postgres.encrypt.encryption.Encrypted;

@Entity
@Table(name = "hmcts_case")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String caseReference;

    @Encrypted
    private String secureText;

    @Encrypted
    @Column(name = "defendent_json")
    private Defendent defendent;
}
