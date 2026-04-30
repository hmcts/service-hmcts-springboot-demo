# postgres-encrypt

Demonstrates transparent field-level encryption for JPA entities using a custom `@Encrypted` annotation.

Sensitive fields are encrypted before they reach the database and decrypted automatically on load — no repository, service, or query code needs to know encryption exists.

---

## How it works

### The annotation

```java
@Encrypted
private String defendantName;
```

That is the only change required on an entity field. No converter, no listener registration, no extra configuration per entity.

### Under the hood

| Component | Responsibility |
|---|---|
| `@Encrypted` | Marks a `String` field as requiring encryption at rest |
| `EncryptionService` | Interface defining `encrypt(String)` / `decrypt(String)` |
| `StubEncryptionService` | Stub implementation — **replace with Azure Key Vault** (see below) |
| `EncryptionEventListener` | Hibernate event listener — encrypts the DB-bound state array on `PreInsert`/`PreUpdate`, decrypts entity fields on `PostLoad` |
| `HibernateListenerRegistrar` | Registers the listener globally at startup so every entity is covered automatically |

The listener intercepts Hibernate's own state array (the values it builds the SQL from) rather than modifying the entity in memory. This means the Java object always holds plain text; only the database column ever sees the encrypted value.

---

## Stub vs real encryption

The current `StubEncryptionService` wraps values in XML-like tags to make the effect visible during development:

| Layer | Value |
|---|---|
| Java (in memory) | `John Smith` |
| PostgreSQL column | `<ENCRYPT>John Smith</ENCRYPT>` |

**This is not real encryption.** It exists purely to make the encrypt/decrypt lifecycle visible without external dependencies.

### Swapping in Azure Key Vault

Create a new `@Service` that implements `EncryptionService` and annotate it `@Primary` (or remove `StubEncryptionService`):

```java
@Service
@Primary
public class AzureKeyVaultEncryptionService implements EncryptionService {

    private final CryptographyClient cryptographyClient;

    @Override
    public String encrypt(String plainText) {
        EncryptResult result = cryptographyClient.encrypt(
            EncryptionAlgorithm.RSA_OAEP,
            plainText.getBytes(StandardCharsets.UTF_8)
        );
        return Base64.getEncoder().encodeToString(result.getCipherText());
    }

    @Override
    public String decrypt(String cipherText) {
        DecryptResult result = cryptographyClient.decrypt(
            EncryptionAlgorithm.RSA_OAEP,
            Base64.getDecoder().decode(cipherText)
        );
        return new String(result.getPlainText(), StandardCharsets.UTF_8);
    }
}
```

No entity, repository, or listener code changes. The `@Encrypted` annotation and the rest of the infrastructure remain identical.

The Azure Key Vault demo will live in a separate module (`azure-vault-encrypt`) once the stub has been validated.

---

## Running locally

Start Postgres:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Run the application:

```bash
./gradlew bootRun
```

---

## Running the tests

Tests use Testcontainers to spin up a real Postgres instance automatically — no local database required.

```bash
./gradlew test
```

`CaseRepositoryTest` verifies two things:

1. A raw JDBC query against the `hmcts_case` table confirms `defendant_name` is **not** stored as plain text.
2. Loading the same row via `CaseRepository` returns the original plain-text value — demonstrating transparent decryption.
