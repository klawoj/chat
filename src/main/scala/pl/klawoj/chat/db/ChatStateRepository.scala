package pl.klawoj.chat.db

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.github.plokhotnyuk.jsoniter_scala.core
import pl.klawoj.chat.domain.ChatShardEntity.BoundToParticularChat.ChatId
import pl.klawoj.chat.domain.ChatShardEntity.OngoingChat
import pl.klawoj.helpers.Ack
import pl.klawoj.helpers.GuavaFutureOpts.guavaFutureOpts
import pl.klawoj.helpers.cassandra.CassandraSession

import java.lang.{Long => JavaLong}
import java.nio.ByteBuffer
import scala.concurrent.{ExecutionContext, Future}

object ChatStateRepository {

  import CassandraSerialization._

  private val session = CassandraSession.session

  def forUser(userId: String)(implicit executionContext: ExecutionContext): Source[OngoingChat, NotUsed] =
    OngoingChatSource(Queries.existingChatsForUser, Seq(userId))

  def forChat(chatId: ChatId)(implicit executionContext: ExecutionContext): Source[OngoingChat, NotUsed] =
    OngoingChatSource(Queries.existingChat, chatId.ids.toSeq).limit(1)


  def insertForUser(userId: String, chat: OngoingChat)(implicit executionContext: ExecutionContext): Future[Ack] = {
    val bind: Seq[Object] = Seq(
      userId,
      chat.createdAt.toEpochMilli: JavaLong,
      ByteBuffer.wrap(core.writeToArray(chat)))
    insert(Queries.statementUpdateChatInfoForUser, bind)
  }

  def insertForChat(chatId: ChatId, chat: OngoingChat)(implicit executionContext: ExecutionContext): Future[Ack] = {
    val bind: Seq[Object] = Seq(
      chatId.id1,
      chatId.id2,
      chat.createdAt.toEpochMilli: JavaLong,
      ByteBuffer.wrap(core.writeToArray(chat)))
    insert(Queries.statementUpdateChatInfo, bind)
  }

  private def insert(incomingStatement: PreparedStatement, bind: Seq[Object])(implicit executionContext: ExecutionContext): Future[Ack] = {
    val statement: BoundStatement = incomingStatement.bind(bind.toArray: _*)
    session.executeAsync(statement).asScala().map(_ => Ack())
  }
}
