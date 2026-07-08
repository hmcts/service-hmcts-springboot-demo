# correlation-id-demo

Demonstrates the full correlation ID lifecycle across an HMCTS service.

## Why correlation IDs matter

A correlation ID is a single UUID that follows a request end-to-end — across HTTP hops, through async queues, into every log line. Without one, tracing a failure across microservices means guessing which log lines belong together. With one, you can filter any log aggregator to a single ID and see the exact chain of events.

## What this demo shows

### 1. Inbound — `TracingFilter`
Runs at highest precedence on every HTTP request (except `/` and `/actuator/*`).
- If `X-Correlation-Id` is present in the request header → uses it.
- If absent → generates a new UUID and logs a warning.
- Puts the ID into SLF4J MDC so it appears automatically in every log line for that request thread.
- Echoes the ID back in the response header so callers can correlate too.
- Clears the MDC in a `finally` block to avoid thread-pool leakage.

### 2. Logging — `logback.xml`
Uses `logstash-logback-encoder` with the `<mdc/>` provider. This means `X-Correlation-Id` is emitted as a structured JSON field on every log event — no manual interpolation needed.

```json
{ "X-Correlation-Id": "abc-123", "timestamp": "...", "message": "Processing request" }
```

### 3. Outbound — `OutboundTracingInterceptor`
A `ClientHttpRequestInterceptor` that reads the correlation ID from MDC and sets it as a header on every outbound `RestClient` / `RestTemplate` call. Register it on your client bean:

```java
RestClient.builder()
    .requestInterceptor(outboundTracingInterceptor)
    .build();
```

This ensures downstream services receive the same ID and can continue the trace.

## What this demo does NOT include (but production services must)

### Queue propagation
When a message is placed on an Azure Service Bus queue, the HTTP request thread ends — so MDC is lost. To preserve the correlation ID:

1. **On send** — embed the correlation ID in the message payload (e.g. a `correlationId` field in the wrapper object).
2. **On receive** — the queue processor extracts the ID from the payload and restores it to MDC before any processing begins, then clears it in a `finally` block.

This pattern is implemented in `service-cp-crime-hearing-results-document-subscription` — see `ServiceBusClientService` (send) and `ServiceBusProcessorService` (receive).

### Async threads
`MDC` is thread-local. If you use `@Async`, `CompletableFuture`, or virtual threads you must copy the MDC context manually before handing off:

```java
Map<String, String> context = MDC.getCopyOfContextMap();
executor.submit(() -> {
    MDC.setContextMap(context);
    try { ... } finally { MDC.clear(); }
});
```
