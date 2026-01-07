# goal of this module is to demonstrate using Azure service bus in a local docker environment

We use docker-compose.yml to bring in 2 docker images
1) mssql - The default backend storage for service-bus
2) servicebus-emulator - The azure emulator

Note that to run mssql on a macbook with apple silicon we need to enable

Docker desktop -> Settings -> General
* Enable "Apple Virtualization Framework"
* Enable "Use Rosetta for x86_64/amd64 emulation on Apple Silicon"

We use gradle plugin com.avast.gradle.docker-compose
We define a gradle task in service-bus.gradle
The docker-compose for servicebus-emulator pulls in service-bus-config.json that defines the following
service-bus components



