# Goal of this demo is to show how to run Karate DSL tests against a docker-compose stack

This is the Karate-flavoured sibling of `api-test-demo` - same `com.avast.gradle.docker-compose`
wiring (`gradle/api-test.gradle`, task `api`), but the single api-test is written as a Karate
`.feature` file instead of a JUnit5 + RestTemplate test.

It does the following
* Spins up docker-compose which builds and runs the app (no database - kept minimal)
* Runs a single Karate feature against the running container to check actuator health is UP

The api-test in this demo hits http://localhost:8083/actuator/health against the running
docker container. See `service-cp-crime-hearing`'s `src/smokeTest` for a fuller example of Karate
against a real deployed environment (multi-step scenarios, OAuth, secret-masking in reports).

We can run the full stack of tests which includes the Karate api-test
```
./gradlew clean build
```

We can run just the Karate api-test on its own (builds and starts the `app` container via
docker-compose, waits for it to become healthy, runs the Karate feature, then tears the stack down)
```
./gradlew api
```