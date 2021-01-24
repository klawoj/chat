
LAUNCHING

There is cassandra database required. To test the solution it is enough to run it quickly in docker:

docker pull bitnami/cassandra:latest
docker run --name cassandra -d -p 9042:9042 bitnami/cassandra:latest

Then connect to the database and create the schema located in

/db/schema.cql

After that you can run the app using sbt

SOLUTION DESCRIPTION

One microservice forming an akka cluster, many instances should allow 10M users....
For that to happen, first a single chat between two users must be modified in a single place in the cluster.
When we identify a chat by a pair of user ids  (a,b)  (let a < b , not to introduce duplicates) we should use it as EntityId in sharding

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



STUFF NOT DONE PROPERLY HERE (OR NOT DONE AT ALL)

-I assume that there is already some load balancer that works on HTTP layer so that requests are more or less evenly distributed among instances of my app
-Java serialization is used which is bad for several reasons
-Cluster has to be properly set up. Currently single node joins itself to form a single element cluster

DOMAIN ASSUMPTIONS
-User identifiers may contain only alphanumeric characters and '-'
-The service does not maintain any user list
-Starting a chat that already exists will return OK, but it won't change app state in any way
-Listing all messages of a chat that does not exists will return empty list
-Users local timestamps are not passed in any way to the server. Messages are stored with server timestamp.