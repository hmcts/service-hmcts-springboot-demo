# Audit Filter — Step 1: Annotations

Spec reference: [CP Audit Message Format — David Edwards, CPPGM](https://tools.hmcts.net/confluence/spaces/CPPGM/pages/1899790847/Audit)

---

## Overview

A Spring Boot `OncePerRequestFilter` that **blocks all requests by default**. Every controller method must declare its audit intent via `@AuditDetail` or `@AuditExclude`. Two audit events are fired per request — one before the chain runs (REQUEST) and one after (RESPONSE).

---

## Annotations

Applied to controller methods. Method-level takes precedence over class-level.

```java
// Fully excludes the endpoint — no event is sent (health checks, actuator etc.)
@AuditExclude

// Declares audit intent — required on every audited endpoint
@AuditDetail(
    origin     = "hearing-results-document",   // default — service deployment name
    component  = "QUERY_API",                  // default — API tier
    eventName  = "hearing-results-document.get-document",  // mandatory
    action     = "Download",                   // default "View"
    pathParams = {"clientSubscriptionId", "documentId"}    // UUIDs to include
)
```

Only the path params explicitly listed in `pathParams` are included. No headers, no body.

---

## Payloads

### REQUEST event (fired before the controller code runs)

```json
{
  "origin":    "hearing-results-document",
  "component": "QUERY_API",
  "timestamp": "2026-07-10T10:30:00.123Z",
  "content": {
    "_metadata": {
      "id":   "3f535641-ed96-4c34-bfca-36b0c19e072e",
      "name": "hearing-results-document.get-document",
      "context": {
        "user": "31ec3a16-8721-498c-8da5-f099390ee254"
      }
    },
    "eventType":     "REQUEST",
    "action":        "Download",
    "correlationId": "b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f",
    "responseStatus": null,
    "pathParams": {
      "clientSubscriptionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "documentId":           "7c9e6679-7425-40de-944b-e07fc1f90ae7"
    }
  }
}
```

### RESPONSE event (fired after the controller code completes)

```json
{
  "origin":    "hearing-results-document",
  "component": "QUERY_API",
  "timestamp": "2026-07-10T10:30:00.456Z",
  "content": {
    "_metadata": {
      "id":   "3f535641-ed96-4c34-bfca-36b0c19e072e",
      "name": "hearing-results-document.get-document",
      "context": {
        "user": "31ec3a16-8721-498c-8da5-f099390ee254"
      }
    },
    "eventType":      "RESPONSE",
    "action":         "Download",
    "correlationId":  "b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f",
    "responseStatus": 200,
    "materialId":     "a1b2c3d4-e5f6-7890-abcd-ef0123456789",
    "pathParams": {
      "clientSubscriptionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "documentId":           "7c9e6679-7425-40de-944b-e07fc1f90ae7"
    }
  }
}
```

> `_metadata.id` is the **same UUID** for both the REQUEST and RESPONSE events of a single HTTP request — it acts as the correlation key linking the two events together. ⚠️ TODO: the current implementation generates a new `UUID.randomUUID()` per event; the request-event UUID needs to be stored and reused for the response event.

> `materialId` (and `caseId`, `hearingId`, `courtDocumentId`) are populated from MDC on the RESPONSE event — set by the service layer via `AuditMdcKeys`. They are absent from the REQUEST event because the handler has not yet run.

> `eventType`, `correlationId`, `responseStatus`, and `pathParams` are HMCTS filter extensions. `content` is extensible per the spec.

---

## Annotation field mapping to spec

| `@AuditDetail` field | Spec field               | Notes                                           |
|----------------------|--------------------------|-------------------------------------------------|
| `origin`             | top-level `origin`       | Service deployment name                         |
| `component`          | top-level `component`    | `QUERY_API`, `COMMAND_API`, etc.                |
| `eventName`          | `content._metadata.name` | Convention: `<service>.<entity>-<verb>`         |
| `action`             | `content.action`         | Human-readable: Download, View, Create, Delete  |
| `pathParams`         | extension                | Named UUID path variables — not headers or body |

---

## Spec compliance

| Spec requirement                    | Status     | Notes                                                                          |
|-------------------------------------|------------|--------------------------------------------------------------------------------|
| FR.01 All user actions recorded     | ✅          | Filter blocks by default; every unexcluded endpoint must declare `@AuditDetail` |
| FR.03 Audit failures block request  | ✅          | Exception in request audit returns 403 — request does not proceed              |
| FR.04 Standard message format       | ✅          | Envelope: `origin`, `component`, `timestamp`, `content._metadata`              |
| FR.05 User attribution only         | ✅          | `@AuditExclude` for health checks / infrastructure endpoints                   |
| FR.06 Audit config visible          | ✅          | All endpoints annotated — exclusions explicit and visible in code              |
| `_metadata.context.user`            | ⚠️ TODO    | Currently from env var `CJSCPPUID` — needs Spring Security context wired in    |

---

## Interaction diagram

```
Client                  AuditFilter                  Handler
  |                          |                           |
  |--- HTTP Request -------->|                           |
  |                   [REQUEST audit event logged]       |
  |                          |--- invoke chain --------->|
  |                          |                    [MDC populated]
  |                          |<-- response --------------|
  |                   [RESPONSE audit event logged]      |
  |<-- HTTP Response --------|                           |
```
