# CP Access Facade — Audit Starter + Demo Service

## Quick start for Audit
1) Add the audit package to build.gradle
2) Enable scanning to pick up the AuditFilter
3) ( Temp include the required packages ... still need to sort this grrr )

# Run the demo service
docker-compose up
gradle :audit-filter-demo:bootRun
```
Access artemis console on http://localhost:8161/console and login with artemis/artemis
See messages landing into the jms.topic.auditing.event Queue

When the demo is running, send some sample requests to generate Audit payloads to artemis

```
curl http://localhost:8080
curl -v -H 'CJSCPPUID: la-user-1' http://localhost:8080/api/hello
```
