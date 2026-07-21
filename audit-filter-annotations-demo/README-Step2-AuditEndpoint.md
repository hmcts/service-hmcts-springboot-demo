# AuditService — Proposed Solution - For discussion only

## Overview

A standalone Spring Boot service that receives audit events from any number of client services via HTTP POST. It validates the inbound payload against a firm schema, writes the event to Artemis JMS in a transacted session, and **does not return a success response until the message is committed to the broker**.

---

## Inbound contract (firm spec)

`POST /audit`

Required fields — request is rejected `400` if any are missing:

| Field | Type | Notes |
|---|---|---|
| `correlationId` | `string` | UUID format |
| `method` | `string` | HTTP verb |
| `path` | `string` | URI path |
| `statusCode` | `int` | HTTP status |
| `timestamp` | `string` | ISO-8601 UTC |
| `service` | `string` | Originating service name |

Optional fields: `pathParams`, `durationMs`, `requestHeaders`, `requestBody`, `responseBody`

---

## Write behaviour

1. Validate payload — return `400` on schema failure (no JMS write attempted)
2. Open a JMS session in **transacted** mode
3. Send message to `audit.events` queue
4. **Commit the transaction** — only here does the method return
5. Return `202 Accepted` to the caller

This guarantees at-least-once delivery: the calling filter thread is unblocked only after the message is durable on the broker.

---

## Error handling

| Scenario | Response |
|---|---|
| Payload validation failure | `400` — caller logs and drops, no retry |
| JMS broker unavailable | `503` — surfaces back through the blocking filter to the original caller |
| Duplicate `correlationId` | `200` — idempotent accept |

---

## Interaction diagram

```
Audit Filter              AuditService              Artemis JMS
      |                        |                         |
      |--- POST /audit ------->|                         |
      |                [validate schema]                 |
      |                [JMS transacted send]------------>|
      |                [commit] <-------------------[ack]|
      |<-- 202 Accepted -------|                         |
```

---

## Open questions

- Maximum payload size limit (suggest 64 KB)?
- Dead-letter queue strategy for messages that fail downstream processing?
- Authentication between filter and AuditService — mTLS or shared secret?
