# Demo repository containing examplar / demonstration projects

Various sub projects to demonstrate small functions in spring boot

The sub projects are deliberately small and clean and illustrate best practice

The sub project support the spring boot template which runs in parallel
https://github.com/hmcts/service-hmcts-crime-springboot-template


## Database
postgres-springboot4
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

api-test-demo
Api test written as a JUnit5 + RestTemplate test against a docker-compose stack

karate-test-demo
Same docker-compose api-test wiring as api-test-demo, but the test is a Karate DSL `.feature`
file instead


## Azure APIM
apim-subscription-key-demo
Create, list, read and delete Azure API Management subscription keys with the Azure SDK and DefaultAzureCredential,
against the real sandbox APIM. Documents the exact Azure RBAC actions a service needs to do it, and the terraform
and workload identity that grant them.


## Entra
entra-node-login-demo
Sign in with Microsoft Entra and show the user's oid, name and email. A tiny Node app, not Spring Boot, so the
OAuth2 authorization code flow stays visible rather than hidden behind framework config. README covers the
front/back channel split and why oid rather than sub.

entra-auth-demo - WIP ?
Spring Boot as an OAuth2 resource server validating Entra bearer tokens, with mock-oauth2-server standing in
for Entra


## Filters
audit-filter
Intercept incoming requests and pass to apache/activemq-artemis

auth-filter
Intended to 


