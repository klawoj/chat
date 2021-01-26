LAUNCHING

Cassandra database is required. To test the solution it is enough to run it quickly in docker:

docker pull bitnami/cassandra:latest
docker run --name cassandra -d -p 9042:9042 bitnami/cassandra:latest

Then connect to the database and create the schema located in

src/test/resources/chat.cql

After that you can run the app using 'sbt run'

If you want the tests to work properly you also need the same cassandra image in your docker registry

API

Look into
/requests/rest-api_chat.http for examples


DOMAIN ASSUMPTIONS

-User identifiers may contain only alphanumeric characters and '-'
-I assume I have access to an UserService that for userId will return userName (if the user exists).
-I will use dummy UserService that has several hardcoded entries as an implementation (do change the code if you need more users)
-Normally I would expect UserService to be another properly scaled microservice so that it can handle 10M users
-Users of my API have to know their IDs to do anything (REST api is based on IDs obviously, not names) so they have to have access to some user list.
-Simple authentication = checking UserService for existence of both users when creating a chat
-Starting a chat that already exists will return OK, but it won't change app state in any way
-Listing all messages of a chat that does not exists will return empty list
-Users local timestamps are not passed in any way to the server. Messages are stored with server timestamp.
-"small snippet of the last message" - I will return/keep the whole message. Only client knows how much is appropriate. Cutting it on server side seems risky.


SOLUTION DESCRIPTION

One microservice forming an akka cluster that can be scaled.
I assume that there is already some load balancer that works on HTTP layer so that requests are more or less evenly distributed among existing instances of my app
To scale properly, first a single chat between two users must be handled/modified in a single place in the cluster.
When we identify a chat by a pair of user ids  (a,b)  (let a < b , not to introduce duplicates) we should use it as EntityId in sharding
Lets call it ChatId

Then 2 out of 4 operations ( StartChat, PostMessage) must be handled that way.
Behind sharding we can keep conversation state in memory.
If we do that we can handle efficiently also GetAllMessagesInChat.

To that point it seemed quite obvious, but one thing that feels wrong is that we don't have an operation that would enable us to deliver the messages instantly to the other user.
If we want to know if there are any new messages we must do GetAllMessagesInChat.
Additional WebSocket operation that would push any new messages to the client would do the trick here, but I won't implement it for now. I want to get right what actually is required.
Because we do not have such operation, I will assume that GetAllMessagesInChat will be called frequently and it is therefore beneficial to keep this state in memory...
This however this assumes that a conversation won't get huge... Let's keep that assumption for now (I have to assume something).
If it however got huge we would not probably want to call GetAllMessagesInChat often, so then we would end up changing the API and the service anyway.


So if we have 1M concurrent chats and 100M messages I could change my solution and do the following:
-not to keep all messages in memory (maybe just n last messages, fetching more only if client explicitly wants more)
-measure actual usage and setup proper timeout for Entity passivation
-introduce GetLastNMessagesInChat(n) or GetMessagesInChatSince(time) operations and force clients to use it instead of GetAllMessagesInChat.
-introduce WebSocket operation mentioned earlier so that message is pushed immediately to the client if user is connected.
-using GetLastNMessagesInChat(n) together with WebSocket connection could in practise eliminate the need to use GetAllMessagesInChat or to keep messages in memory,

So the last operation is GetAllUserChats. This one I will simply delegate to be streamed from storage (table dedicated for that query).

You can scale by adding instances of chat app and more instances to cassandra cluster.


STORAGE

1) Cassandra table for chat messages, where partition key is our ChatId tuple, and clustering key is message creation time.
Ordered so that most recent message comes first
2) Cassandra table for marking a conversation as ongoing, and keeping conversation state, where partition key is our ChatId tuple, and we will have at most one row for ChatId (only when the conversation is ongoing)
3) Cassandra table for easy querying ongoing conversations for particular user, where partition key is 'userId', and clustering key is chat creation time.

Tables described above should allow efficient data retrieval for all required use cases
So in case of creating a chat, 3 rows will be created:
Table 2) Conversation state - new row,
Table 3) Conversation state kept per user - new row (for both users)
And in case of posting a message, 1 row will be created and 3 rows updated.
Table 1) new row with message,
Table 2) Conversation state - update row with last message,
Table 3) Conversation state kept per user - update row with last message (for both users)



STUFF NOT DONE PROPERLY (OR NOT DONE AT ALL) BECAUSE OF LIMITED TIME

-Search for 'TODO's in the code. Most of the 'missing' stuff is described there
-Java serialization is used, which is bad for multiple reasons
-Akka cluster has to be properly set up. Currently single node joins itself to form a single element cluster.
-Same with cassandra cluster
-Logging is barely touched
-Haven't really spend time on akka configuration and cassandra connector configuration here.
-I am not happy with test coverage. I have one unit test for REST API (happy paths only) and second test for DomainOperations + its integration with cassandra, which uses docker.
This actually covers the flow and may be good enough here because the logic is really simple, but the second test has too big scope.
There should be also smaller tests for: serialization, conversions, logic. etc



