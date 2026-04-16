# Azure self hosted gateway ?
#
docker run -d -p 80:8080 -p 443:8081 --env-file env.conf mcr.microsoft.com/azure-api-management/gateway:latest

docker run -d -p 80:8080 -p 443:8081 mcr.microsoft.com/azure-api-management/gateway:latest