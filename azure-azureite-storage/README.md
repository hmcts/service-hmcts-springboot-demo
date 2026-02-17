# Azure azureite is a docker emulator that can be used to locally test Azure file storage

# An example using TestContainers to spin up azureite docker container in integration tests can be found at
https://github.com/hmcts/cp-case-document-knowledge-service

Uses avast docker-compose plugin for gradle here
https://github.com/hmcts/cp-case-document-knowledge-service/blob/main/build.gradle

With gradle docker compose config here
https://github.com/hmcts/cp-case-document-knowledge-service/blob/main/gradle/docker.gradle

And docker compose here
https://github.com/hmcts/cp-case-document-knowledge-service/blob/main/docker/docker-compose.integration.yml

( Note this project also uses artemis whic may be useful example )