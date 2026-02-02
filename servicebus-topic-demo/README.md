# Goal of this module is to demonstrate using Azure service bus for Topic and Subscription

We use topic and subscription to send queeud messages to multiple subscribers.
See associated readme in servicebus-queue-demo ( single throughput queue )

We demo the following capabilities
* Single send to topic and multiple subscriptions notified
* Subscription send failure increase failure count and move to DLQ
* Reprocess from DLQ
* Purge subscription queue and DLQ queue ( especially for test )
* Send many messages and multi thread processing

We currently have the following niggles in our demo
* Cannot do topic admin such as add subscriptions or get-count
* When sending more than 9 messages it just hangs. Odd.


# Todo
Multiple threads processing
Oddly the demo tries to send many messages and then process them in threads to prove locking for multi threading
Sending more than 10 messages synchronously seems to block / hang.
The first 10 messages send in around 100msecs.

How create subscriber on topic programmatically. This requires admin functions which apparently is almost here
https://techcommunity.microsoft.com/blog/messagingonazureblog/introducing-administration-client-support-for-the-azure-service-bus-emulator/4486433




