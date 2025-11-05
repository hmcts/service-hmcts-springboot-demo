# Postgres Lock

Simple demonstration how we can use postgres locking to guarantee once only operations on database items
Baeldung example here https://www.baeldung.com/jpa-pessimistic-locking

i.e. We can use this to ensure only 1 thread can operate on a single table row
Useful for processing audit rows where it possible there may be multiuple nstances with multiple threads
processing the same table

In summary, we use the following native postgres query to lock a data row
"select * from audit limit 1 FOR UPDATE"

And we have an integration test that loads 100 Audit entries
And processes them Asynchronously
Using the query with the "for update" we send exactly 100 audits
If we use the query without the "for update" we get errors because the audit row is not locked
And we try to delete a row that has already been deleted, thus throwing an error
