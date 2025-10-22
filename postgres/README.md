# Postgres Best practice

This module is a minimal demonstration using jpa and postgres in a springboot application

In particular
* Using flyway to maintain the database schema for every level including spring boot tests
* Using @TestContainer to access a docker postgres database for integration tests
* Showing simple jpa Entity
* Showing jpa query language NOT using native sql
* Using mapstruct to easily map between the repository layer and the controller layer
* Repository test to prove that the flyway creates a database schema that lines up with the jpa layer


We need docker running with postgres database with a db user and password that line up
with our connection settings in application.yml
i.e. We use simple db=postgres and user/password of postgres/postgres

Suggest running docker postgres through docker-compose.yml
i.e. 
```
docker-compose -f docker/docker-compose.yml up
```

Note that flyway is immutable, with the current state held in flyway_schema_history table
If we amend flyway scripts that have been applied, we will get an error similar to
```
Migration checksum mismatch for migration version 1.001
-> Applied to database : -2092978026
-> Resolved locally    : -1889185514
```
We get around this by dropping the database ... which means a drop of the docker postgres db container in this implementation