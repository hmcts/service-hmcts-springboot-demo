# Demo repository containing examplar / demonstration projects

Various sub projects to demonstrate small functions in spring boot

The sub projects are deliberately small and clean and illustrate best practice

The sub project support the spring boot template which runs in parallel
https://github.com/hmcts/service-hmcts-crime-springboot-template


## Database
postgres-springboot3 / postgres-springboot4
Flyway, java persistence, jpa repository, jpa entities and integration test with TestContainer / PostgreSQLContainer 

postgres-lock
Flyway, java persistence, native Query, postgres row locking and integration test with TestContainer / PostgreSQLContainer


## Queue
servicebus-queue, servicebus-topic
Azure service bus with local azure emulator in docker. Integration test with avast docker-compose to spin up emulator in docker
Queue is a single fan out
Topic provides single publish multiple subscribers


## Testing 
See 3 types of docker container integration tests
TestContainer with standard PostgresSQLContainer - simplest implementation with off the shelf test container
TestContainer with custom GenericContainer wrapper - allows implementation of custom test container
Docker with avast docker-compose - allows spin up containers using docker-compose embedded in gradle


## Filters
audit-filter
Intercept incoming requests and pass to apache/activemq-artemis

auth-filter
Intended to 


