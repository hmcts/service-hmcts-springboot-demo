# Demo http client implementation using spring boot feign http client 

See baeldung intro-to-feign overview

We use the feign client to generate http client classes to interact with other rest endpoints
( Rather than using other rest clients such HttpClient, RestClient, RestTemplate )

We have a simple interface Class DemoClient

And a spring boot integration test that demonstrates how the DemoClient is invoked
i.e. Http Client is as simple as ...
```
DemoResponse response = demoClient.getDemoById(1L);
```

We also use wiremock to mock the response from the target endpoint in our integration test