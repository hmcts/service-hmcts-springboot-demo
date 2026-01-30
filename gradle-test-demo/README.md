# Demo to illustrate how we setup gradle to run our tests

We have 4 types of Test in our application, plus apiTest which is outside our application

## unitTest - A lightweight test typically testing across a single layer that mocks the underlying layer.
i.e. Controller test mocks the service layer and calls all exposed methods in the controller.
The unitTest should test all code branches

## unitSpringBootTest / repositoryTest- A unit test that requires spring boot stack because of inherent complexities
i.e. Jpa repository needs to integrate tightly with postgres running as @TestContainer and flyway
Unit tests are difficult and maybe pointless.
We need to ensure that we test that the jpa repository, entities and flyway created postgres tables all line up
Any changes to flyway are tested by the repository tests
We may have just a simple repository test that saves a jpa entity to postgres database table and then reads back the entity
i.e. where we just default repository methods such as save() and findById() then a simple repository test is sufficient
We do require repository tests for any custom SQL  

## integrationTest - SpringBoot test without docker
integrationDockerTest ( with docker compose up )

We need the different test types to 
a) Run on selection in intellij
b) Show logging ( from app and tests )
c) Run for the correct gradle task i.e. "gradle test" "gradle integrationTest"


## dockerTest - An spring boot integration test that requires docker stack


1) Unit tests 
Are lightweight mocking tests
We typically mock the layer beneath
We test every branch in the code
i.e. Controller we mock the service underneath

Mapping tests are typically exhaustive
i.e. We test every field and every variation

The simple pattern we use is
* Use @ExtendWith(MockitoExtension.class) 
* Use @Mock for any mock services injected into the class-under-test ( most services )
* Use @Spy to inject any real services into the class-under-test ( such as mappers )
* Use @InjectMocks to instantiate the service under test with @Mocks and @Spys
* Use when(service.method()).thenReturn(mock)
* use verify to confirm that mocked services have been executed with the correct parameters
* Use assertThat to confirm daya is as expected

2) Repository Integration tests

3) Spring Boot Integration tests
Are spring boot tests thus heavy weight because of the construction of the spring stack and beans
We use flyway to create any database tables required
We try to have a single spring boot stack with the same set of beans with behaviour varied using app.properties
We try to drive a pin through the whole stack from controller down to repository or client calls
* Using @TestContainers where possible for any backend services such as postgres
* Creating any database data required for our test
* Mocking any backend service endpoints that we hit
* Hitting an api endpoint to start the test
* Using assertThat any data is returned as expected
* Using verify to ensure any mock clients are called with expected parameters
Our spring boot integration tests do not need to test every code branch. They are intended to test the 
integration between the layers