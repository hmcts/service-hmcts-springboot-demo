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


## Entra and APIM

entra-apim-registration-demo
The API Marketplace credential journey, runnable on a laptop: sign in with Entra, register an application to get a Client ID
and Client Secret (Graph), connect an API to get a Subscription Key (APIM), and take them away again.
Entra runs in docker as mock-oauth2-server, Graph and APIM as a WireMock stand-in.

entra-auth-demo
Spring Boot as an OAuth2 resource server validating Entra bearer tokens, with mock-oauth2-server standing in for Entra


## Filters
audit-filter
Intercept incoming requests and pass to apache/activemq-artemis

auth-filter
Intended to 


