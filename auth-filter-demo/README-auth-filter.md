# CP Access Facade — Audit Starter + Demo Service

## Quick start for Audit
1) Add the audit package to build.gradle
2) Enable scanning to pick up the AuditFilter in the same package i.e. uk.gov.hmcts with @EnableAutoConfiguration
3) Enable jms to interact with artemis ( maybe we can move this into lib )
   @EnableJms

# Run the demo service
docker-compose up
gradle bootRun
curl http://localhost:8080
```
1) Confirm the logging from the AuditService 
2) Access artemis console on http://localhost:8161/console and login with artemis/artemis
See messages landing into the jms.topic.auditing.event Queue
