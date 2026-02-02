# Goal of this module is to demonstrate using Azure service bus for Topic and Subscription

We use topic and subscription to send queeud messages to multiple subscribers.
See associated readme in servicebus-queue-demo ( single throughput queue )

We demo the following capabilities
* Single send to topic and multiple subscriptions notified
* Subscription send failure increase failure count and move to DLQ
* Reprocess from DLQ
* Purge subscription queue and DLQ queue ( especially needed for test )
* Send many messages with multi thread processing
* Admin such as query / add / delete queue or topic+subscriptions


We currently have the following niggles in our demo
* Unable to get a queue count or subscription count

# Todo
Multiple threads processing
Oddly the demo tries to send many messages and then process them in threads to prove locking for multi threading
Sending more than 10 messages synchronously seems to block / hang.
The first 10 messages send in around 100msecs.

How create subscriber on topic programmatically. This requires admin functions which apparently is almost here
https://techcommunity.microsoft.com/blog/messagingonazureblog/introducing-administration-client-support-for-the-azure-service-bus-emulator/4486433




