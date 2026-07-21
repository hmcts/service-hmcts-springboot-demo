# Audit Proposed Solution — Summary

> **Work in progress.** The immediate intent is to send audit events for the `hrds` `get-document` endpoint. The annotation-driven approach is deliberately extensible — other endpoints may require capturing information from headers or request/response bodies, which will need additional filter support as those use cases emerge.

Spec reference: [CP Audit Message Format — David Edwards, CPPGM](https://tools.hmcts.net/confluence/spaces/CPPGM/pages/1899790847/Audit)

---

## Step 0 — Current

The existing `cp-audit-filter-springboot` library is in use. It captures and forwards partial request/response bodies with no per-endpoint control.

Known problems that drive this work:

- **a)** Whole headers and bodies are sent — security risk and PII exposure at the filter level
- **b)** No per-endpoint configuration — every endpoint is treated identically with no way to declare what to capture
- **c)** Hard reliance on the OpenAPI spec to extract path parameter names
- **d)** Hard dependency on Artemis JMS inside the filter — couples every consuming service to the broker

---

## Step 1 — Annotations

See: [README-Step1-Annotations.md](README-Step1-Annotations.md)

Replace the existing filter with one that blocks by default and requires every controller method to declare its audit intent via `@AuditDetail` or `@AuditExclude`. Only explicitly declared path params are included — no headers, no body. Eliminates problems a), b), and c).

---

## Step 2 — Audit Endpoint

See: [README-Step2-AuditEndpoint.md](README-Step2-AuditEndpoint.md)

A standalone service that owns the JMS dependency. Receives audit payloads over HTTP, validates against a firm inbound schema, and writes to Artemis JMS in a transacted session. **Does not return `202` until the message is committed to the broker**, guaranteeing at-least-once delivery.
