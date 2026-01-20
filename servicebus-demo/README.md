# goal of this module is to demonstrate using Azure service bus in a local docker environment


# Rosetta macbook translation layer
Note that to run mssql on a macbook with apple silicon we need to enable Rosetta ms emulator
( This allows us to run docker images that were built for older intel chips such as microsoft azure stuff )

Docker desktop -> Settings -> General
* Enable "Apple Virtualization Framework"
* Enable "Use Rosetta for x86_64/amd64 emulation on Apple Silicon"

Some of our integration tests use TestContainers ( such as postgres ) which is our preferred docker implementation
The TestContainer is configured by a config class such as TestContainersInitialise.class
Which is injected by spring boot annotation i.e. @ExtendWith(TestContainersInitialise.class)

Some components may not be available as TestContainers in which case we use docker-compose to spin up the docker stack
Or some components may be available as TestContainers but they cannot be easily made to work such as Azure ServiceBus.

# Docker Compose
We use gradle plugin com.avast.gradle.docker-compose
We define a gradle task in service-bus.gradle
The docker-compose for servicebus-emulator pulls in service-bus-config.json that defines the following
service-bus components

It takea approximately 60-90 seconds to run docker-compose
The first time we run a service-bus to round trip messages it takes around 20 seconds
Subsequent runs only take a second. Dont know why this is.


