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

# Service Bus testing steps

## 1. Start Service Bus Emulator
cd service-hmcts-springboot-demo/servicebus-demo/ 
docker compose -f docker/docker-compose.yml up -d

Wait for emulator to be ready:
docker logs -f microsoft-azure-servicebus-emulator-servicebus-emulator-1

### 2. Set Connection String
export SERVICE_BUS_CONNECTION="Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"

### 3. Start the Application
cd service-hmcts-springboot-demo/servicebus-demo
./gradlew bootRun or Manually run Application.java

## Testing Queue Operations
---------------------------

# 1. Send a Message
curl -X POST http://localhost:8080/test/queue/send \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello from Service Bus!"}'

# 2. Receive Messages
curl "http://localhost:8080/test/queue/receive?maxMessages=10"

# 3. Send & Receive
curl -X POST http://localhost:8080/test/queue/send-and-receive \
  -H "Content-Type: application/json" \
  -d '{"message": "Quick Test"}'

# 4. Health Check
curl http://localhost:8080/test/queue/health

## Running Tests

### Unit Tests (No Emulator Needed)
./gradlew test --tests AmpServiceBusTest

### Integration Tests (Requires Emulator)
export SERVICE_BUS_CONNECTION="Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"

# Run integration test (starts emulator automatically)
./gradlew integrationTest





