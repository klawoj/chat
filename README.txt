
One microservice forming an akka cluster, many instances should allow 10M users....
For that to happen, first a single chat between two users should be handled by a node which can be easily identified.
When we identify a chat by a pair of user ids  (a,b)  (let a < b, not to introduce duplicates) we can use it as EntityId in sharding

Then 3 out of 4 messages can be handled that way....





Stuff that is obviously done wrong here or omitted:

Java serialization is used which is bad for several reasons
