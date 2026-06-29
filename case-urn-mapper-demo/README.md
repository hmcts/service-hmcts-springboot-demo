# case-urn-mapper-demo

Demonstrates how to consume the CP URN mapper service, which translates a `caseUrn` into a `caseId`.

The module consumes the published artifact `uk.gov.hmcts.cp:api-cp-caseadmin-case-urn-mapper` and
wires it through a simple service layer.

## What it does

`ExampleConsumingService` calls `CaseUrnMapperClient`, which uses a Spring `RestClient` to call:

```
GET {basepath}/urnmapper/{caseUrn}
```

returning a `CaseMapperResponse` containing `caseId` (UUID) and `caseUrn`.

The `basepath` is configured via:

```yaml
case-urn-mapper-client:
  basepath: ${AMP_BACKEND_URL:http://localhost:8090}
```

## Integration tests

There are two integration test strategies — pick one for your own project:

### 1. WireMock — `CaseUrnMapperWireMockIntegrationTest`

Starts a real HTTP server on `localhost:8090` and stubs responses. Tests the full HTTP serialisation
path (JSON → `CaseMapperResponse`) without hitting a real service. Good for verifying wire format.

### 2. Mocked RestClient — `CaseUrnMapperMockedRestClientIntegrationTest`

Uses `@MockitoBean` to replace the `RestClient` bean in the Spring context. Faster than WireMock and
verifies the full Spring injection chain (`ExampleConsumingService` → `CaseUrnMapperClient` → `RestClient`)
without any network calls. Good for verifying wiring and service logic.

## Functional test — hits real dev environment

### `CaseUrnMapperDevIntegrationTest`

Calls the real dev environment at `https://devamp01.ingress01.dev.nl.cjscp.org.uk`. Only runs on
macOS (`@EnabledOnOs(OS.MAC)`) to prevent CI execution — as it requires VPN access into nonlive Dev.

Uses `TrustAllSslRestClientConfig` to disable SSL verification, since the dev cert is self-signed.
In real deployed environments this is not needed — the `$BASE_IMAGE` Dockerfile base includes the CA
that trusts the self-signed cert.

> **Note:** the test case URN `28DI9045455` is real dev data and may change over time.

## SSL in dev

| Environment | SSL handling |
|---|---|
| Local Mac (dev test) | `TrustAllSslRestClientConfig` disables verification |
| Deployed container | `$BASE_IMAGE` includes the CA — no override needed |
| CI (Linux) | Dev test skipped via `@EnabledOnOs(OS.MAC)` |
