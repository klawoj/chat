DOMAIN ASSUMPTIONS
-User identifiers may contain only alphanumeric characters and '-'
-I assume I have access to an UserService that for userId will return userName if the user exists
-I will use dummy UserService that has several hardcoded entries as an implementation
-Users of my API have to know their IDs to do anything (REST api is based on IDs not names) so they have to have access to some user list
-Starting a chat that already exists will return OK, but it won't change app state in any way
-Listing all messages of a chat that does not exists will return empty list
-Users local timestamps are not passed in any way to the server. Messages are stored with server timestamp.
-"small snippet of the last message" - I will return the whole message. Only client knows how much is appropriate. Cutting it on server side seems risky.

LAUNCHING

Cassandra database is required. To test the solution it is enough to run it quickly in docker:

docker pull bitnami/cassandra:latest
docker run --name cassandra -d -p 9042:9042 bitnami/cassandra:latest

Then connect to the database and create the schema located in

/db/schema.cql

After that you can run the app using sbt

SOLUTION DESCRIPTION

API



SCALING

One microservice forming an akka cluster, many instances should allow 10M users....
For that to happen, first a single chat between two users must be modified in a single place in the cluster.
When we identify a chat by a pair of user ids  (a,b)  (let a < b , not to introduce duplicates) we should use it as EntityId in sharding
Lets call it ChatId

Then 2 out of 4 operations ( StartChat, PostMessage) must be handled that way.
Behind sharding we can keep conversation state in memory.
If we do that we can handle efficiently also GetAllMessagesInChat.

To that point it seemed quite obvious, but one thing that feels wrong is that we don't have an operation that would enable us to deliver the messages instantly to the other user.
If we want to know if there are any new messages we must do GetAllMessagesInChat.
Additional WebSocket operation that would push new messages to the client would do the trick here, but I won't implement it for now. I want to get right what actually is required.
Because we do not have such operation, I will assume that GetAllMessagesInChat will be called frequently and it is therefore beneficial to keep this state in memory...
This however this assumes that a conversation won't get huge... Let's keep that assumption for now (I have to assume something).
If it however got huge we would not probably want to call GetAllMessagesInChat often, so then we would end up changing the API and the service anyway.
Another option is to change GetAllMessagesInChat to have optional 'postedLaterThan' parameter...

So the last operation is GetAllUserChats. This one I will simply delegate to be streamed from storage.

STORAGE

-Cassandra table for chat messages, where partition key is our ChatId tuple, and clustering key is message creation time.
Ordered so that most recent message comes first
-Cassandra table for marking a conversation as ongoing, and keeping conversation state, where partition key is our ChatId tuple, and we will have at most one row for ChatId (only when the conversation is ongoing)
-Cassandra table for easy querying ongoing conversations for particular user, where partition key is 'userId', and clustering key is chat creation time.

Tables below should allow efficient data retrieval. In case of posting a message 4 rows in 3 tables will be

STUFF NOT DONE PROPERLY (OR NOT DONE AT ALL) BECAUSE OF LIMITED TIME

-Search for 'TODO's in the code. Most of the 'missing' stuff is described there
-I assume that there is already some load balancer that works on HTTP layer so that requests are more or less evenly distributed among instances of my app
-Java serialization is used which is bad for multiple reasons
-Cluster has to be properly set up. Currently single node joins itself to form a single element cluster which is.
-Logging is not touched at all


