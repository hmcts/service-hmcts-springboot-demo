# Postgres Lock

Simple demonstration how we can use postgres locking to guarantee once only operations on database items

i.e. We can use this to ensure only 1 thread will operate on a single table row
Useful for processing audit rows where it is likely that there may be multiple instances with multiple threads
processing the same table

In summary, we use the following native postgres query to lock a data row
"select * from audit order by id limit 1 FOR UPDATE"

We prove it with an integration test that loads 100 Audit entries
And then processes them Asynchronously
Using the query with the "for update" we send exactly 100 audits
If we use the query without the "for update" we get errors because the audit row is not locked
And we try to delete a row that has already been deleted, thus throwing an error
