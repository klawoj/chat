drop
    keyspace "chat";

create
    keyspace "chat" with replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
    };

create table "chat"."chat_messages"
(
    userId1      text,
    userId2      text,
    createdAt    bigint,
    message_json blob,
    PRIMARY KEY ((userId1, userId2), createdAt)
) with clustering order by (createdAt desc);


create table "chat"."existing_chats"
(
    userId1        text,
    userId2        text,
    createdAt      bigint,
    chat_data_json blob,
    PRIMARY KEY ((userId1, userId2), createdAt)
) with clustering order by (createdAt desc);


create table "chat"."existing_chats_for_user"
(
    userId         text,
    createdAt      bigint,
    chat_data_json blob,
    PRIMARY KEY (userId, createdAt)
) with clustering order by (createdAt desc);
