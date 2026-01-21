# Demo to illustrate how we setup gradle to run our tests

We need the different test types to 
a) Run on selection in intellij
b) Show logging ( from app and tests )
c) Run for the correct gradle task i.e. "gradle test" "gradle integrationTest"


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