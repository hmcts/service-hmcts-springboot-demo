# CP Access Facade — Audit Starter + Demo Service

## Quick start for Audit
1) Add the audit package to build.gradle
2) Enable scanning to pick up the AuditFilter in the same package i.e. uk.gov.hmcts with 
@EnableAutoConfiguration


# Run the demo service
docker-compose up
gradle bootRun
curl http://localhost:8080
```
1) Confirm the logging from the AuditMessageConsumer which proves that the message filter has been applied
And that the message has been sent to artemis

2) If the AuditMessageConsumer is removed, the messages can be seen in the artemis console on 
http://localhost:8161/console and login with artemis/artemis
We can see messages landing into the jms.topic.auditing.event Queue
