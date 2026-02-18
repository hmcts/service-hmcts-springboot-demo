# how to implement retry with exponential backoff

Surely this Should be easy right ? Sadly not. Its not so easy to set backoff retries
There does not seem to be an easy way of setting retry configuration

We can easy set the future processing time for a message when we add it to the queue

The best ( but not atomically safe ) option seems to be to requeue the message in the future

Option 1
Doing this from synchronous processor which has a failure errorHandler is not easy because we dont get the message context, only an error context
So not an option.

Option 2
Wonder if there is a way of doing this async

Option 3
If we set the tries to 1 and let the message go to the DLQ.
We can then pick the message of the DLQ and re-add it to the queue with the future time
But we will need to store the try count inside the message :(

