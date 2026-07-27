# postgres-encrypt

Demonstrates transparent field-level encryption for JPA entities using a custom `@Encrypted` annotation.

Sensitive fields are encrypted before they reach the database and decrypted automatically on load — no repository, service, or query code needs to know encryption exists.

---

## How it works

### Encrypting a String field

```java
@Encrypted
private String secureText;
```

That is the only change required. No converter, no listener registration, no extra configuration per entity.

### Encrypting an object field

Any object can be encrypted as JSON by implementing `JsonEncryptable` and adding `@Column` to name the column:

```java
@Encrypted
@Column(name = "defendent_json")
private Defendent defendent;
```

```java
public class Defendent implements JsonEncryptable {
    private String name;
    private LocalDate dateOfBirth;
}
```

The object is serialised to JSON, encrypted, and stored as a single `text` column. On load it is decrypted and deserialised back to the original type — transparently.

---

## Under the hood

| Component | Responsibility |
|---|---|
| `@Encrypted` | Marks a field as requiring encryption at rest |
| `JsonEncryptable` | Marker interface — implement this on any class to enable object encryption |
| `EncryptionService` | Interface defining `encrypt(String)` / `decrypt(String)` |
| `StubEncryptionService` | Stub implementation — **replace with Azure Key Vault** (see below) |
| `EncryptionEventListener` | Hibernate listener — encrypts `String` fields on `PreInsert`/`PreUpdate`, decrypts on `PostLoad` |
| `JsonEncryptableConverter` | JPA `AttributeConverter` — serialises any `JsonEncryptable` to JSON, encrypts it, and auto-applies to all implementing types |
| `HibernateListenerRegistrar` | Registers the listener globally at startup so every entity is covered automatically |

The listener intercepts Hibernate's own state array (the values it builds the SQL from) rather than modifying the entity in memory. The Java object always holds plain text; only the database column ever sees the encrypted value.

---

## Stub vs real encryption

The current `StubEncryptionService` wraps values in XML-like tags to make the effect visible during development:

| Layer | String field | Object field |
|---|---|---|
| Java (in memory) | `John Smith` | `Defendent{name="Jane Doe", ...}` |
| PostgreSQL column | `<ENCRYPTED>John Smith</ENCRYPTED>` | `<ENCRYPTED>{"type":"...Defendent","data":{...}}</ENCRYPTED>` |

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

`CaseRepositoryTest` verifies in a single test:

1. Raw JDBC queries against `hmcts_case` confirm both `secure_text` and `defendent_json` are **not** stored as plain text.
2. Loading the same row via `CaseRepository` returns the original plain-text values — demonstrating transparent decryption for both a `String` field and an object field.
