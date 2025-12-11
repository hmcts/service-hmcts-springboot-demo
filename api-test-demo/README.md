# Goal of this demo is to show how we run api-tests in our projects

We currently spin up a docker file of the application which is compiled in real time
This uses 2 gradle files apitest.gradle and docker.,gradle
And docker-compose.yml which contains app container and postgres container definitions

We can run the full stack of tests which includes api-test
```
gradle clean build
```


We can run just the api-test with
```
gradle api-test
```


