package pl.klawoj.chat.db

import com.datastax.driver.core.{ConsistencyLevel, PreparedStatement}
import pl.klawoj.helpers.cassandra.{CassandraConfig, CassandraSession}

object Queries {
  private val schemaName = CassandraConfig.load().schemaName
  private val session = CassandraSession.session

  private val chatMessagesTableName = s"$schemaName.chat_messages"
  private val existingChatsTableName = s"$schemaName.existing_chats"
  private val existingChatsForUserTableName = s"$schemaName.existing_chats_for_user"

  val messagesForChat: PreparedStatement = session.prepare(
    s"""SELECT message_json
        FROM $chatMessagesTableName
        WHERE userId1 =?
          AND userId2 =?""")
    .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

  val existingChat: PreparedStatement = session.prepare(
    s"""SELECT chat_data_json
        FROM $existingChatsTableName
        WHERE userId1 =?
          AND userId2 =?
          LIMIT 1""")
    .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

  val existingChatsForUser: PreparedStatement = session.prepare(
    s"""SELECT chat_data_json
        FROM $existingChatsForUserTableName
        WHERE userId =?""")
    .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)


  val statementInsertNewChatMessage: PreparedStatement = session.prepare(
    s"""INSERT INTO $chatMessagesTableName(userId1, userId2, createdAt, message_json)
        values (?, ?, ?, ?)""")
    .setIdempotent(true)
    .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

  val statementUpdateChatInfo: PreparedStatement = session.prepare(
    s"""INSERT INTO $existingChatsTableName(userId1, userId2, createdAt, chat_data_json)
        values (?, ?, ?, ?)""")
    .setIdempotent(true)
    .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)


  val statementUpdateChatInfoForUser: PreparedStatement = session.prepare(
    s"""INSERT INTO $existingChatsForUserTableName(userId, createdAt, chat_data_json)
        values (?, ?, ?)""")
    .setIdempotent(true)
    .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)


}