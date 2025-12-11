# Goal of this demo is to show how we run api-tests in our projects

We currently spin up a docker file of the application which is compiled in real time
This uses 2 gradle files apitest.gradle and docker.gradle ( we should combine them )
And docker-compose.yml which contains app container and postgres container definitions

The api-test in this demo is a simple actuator test
Which hits http://localhost:8082:/actuator/info against the running docker containers


We can run the full stack of tests which includes api-test
```
gradle clean build
```
This runs unit tests, integration tests and api tests.


We can run just the api-test with
```
gradle api-test
```


