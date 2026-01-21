# goal of this module is to demonstrate using Azure service bus in a local docker environment

# Issues to resolve ?
1) How to nicely separate unit-test integration-test
So that we can a) Run tests in idea b) run tests locally c) run tests in pipeline
2) Tons of logging from docker mssql and servicebus but not seeing any spring boot logging


# Rosetta macbook translation layer
Note that to run mssql on a macbook with apple silicon we need to enable Rosetta ms emulator
( This allows us to run docker images that were built for older intel chips such as microsoft azure stuff )

Docker desktop -> Settings -> General
* Enable "Apple Virtualization Framework"
* Enable "Use Rosetta for x86_64/amd64 emulation on Apple Silicon"


# Test Containers ? 
Some of our integration tests use TestContainers ( such as postgres ) which is our preferred docker implementation
The TestContainer is configured by a config class such as TestContainersInitialise.class
Which is injected by spring boot annotation i.e. @ExtendWith(TestContainersInitialise.class)
This makes testing in intellij much easier as the test can be run in isolation, with requiring any gradle operation

Some components may not be available as TestContainers in which case we use docker-compose to spin up the docker stack
Or some components may be available as TestContainers but they cannot be easily made to work such as Azure ServiceBus.
If anybody can get TestContainers working with AzureServiceBus please update this demo module


# Docker Compose
We use gradle plugin com.avast.gradle.docker-compose
We define a gradle task in service-bus.gradle
The docker-compose for servicebus-emulator pulls in service-bus-config.json that defines the following
service-bus components

It takea approximately 60-90 seconds to run docker-compose
The first time we run a service-bus to round trip messages it takes around 20 seconds
Subsequent runs only take a second. Dont know why this is.


