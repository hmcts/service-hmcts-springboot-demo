# Goal of this demo is to show how we run api-tests in our projects

The gradle task api-test runs after successful integration tests

( We need to make it use the same Docker image that will be pushed ... bit this is TODO )
It does the following
* Spins up docker-compose which has postgres and a build of the app
* Builds the app into docker container and runs it
* Note that app has a single flyway which is run against the postgres db thus proving connection
between the app and postgres
* Runs a single ActuatorApiTest against the docker app to check it is UP


The api-test tasks uses gradle file apitest.gradle
And docker-compose.yml which contains app container and postgres container definitions


The api-test in this demo is a simple spring boot actuator test
Which hits http://localhost:8082:/actuator/info against the running docker containers
We could make the api-test another test framework such as cucumber or karate


We can run the full stack of tests which includes api-test
```
gradle clean build
```
This runs unit tests, integration tests and api tests.


We can run just the api-test with
```
gradle api-test
```


