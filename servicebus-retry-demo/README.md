# Goal of this module is to demonstrate retry for Azure Serrvice Bus

Warning 
... its nasty theres no built in way of doing retry with backoff
... we rely on requreuing the message so its not an atomic solution. 
Its "possible" that we could requeue the new before the current message is removed
( The gap between the 2 actions is a few milli secs if the box crashed during this time we could have duplicate message )

If the receiver error is passed up to service bus handler, the error method does not have access to the body
( Or a failure count )

So we end up catching errors in the process message so that we have the message body that we can 
increment the retry count in the body

And then we create a new queued message for the future, where the elapsed time is derived from the failureCount



