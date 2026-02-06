# Goal of this module is to demonstrate using openapi client

We are api first and consistently use openapi spec generated api servers.
This means we generate the endpoint interface and request / response objects. 
Thus guaranteeing that we obey the api-spec contracts

We implement rest clients in various ways. We would like to demonstrate a common best practice approach.
Using a common approach makes it easy to reuse / copy code and importantly tests between projects.

There are various options
* RestClient
* RestTemplate
* WebClient
* HttpClient
* FeignClient
* OpenApiClient

Our preferred option should be OpenApiClient ... this means we simply take the spec and generate the clients and request response objects.

Where should we generated them ? For now lets generate them in our service

## Limitations of openapi generation
We have 2 issues with openapi generation

1) Openapi generated client objects / models do not have @Builder
We have tried to fix this with  additionalModelTypeAnnotations      : "@lombok.Builder"
But this has caused knock on issues and so far we have not come up with a solution :(


2) OpenApi generated server objects still have setters
At least we can create an @Builder annotation :)
But we have been unable to remove the setters :(


## Testing against json files
We would like to be able to mock the response from endpoints that we call using json files with strings.
We prefer not to create actual objects such as those generated from specs
This allows us to folly test the exact examples that we have provided
i.e. An endpoint may return non compliant json or json that drifts from the spec.

It would be ideal if we could get java objects returned from the openapi clients
And mock the underlying openapi client with a json string.

We should do this with wiremock as in this example