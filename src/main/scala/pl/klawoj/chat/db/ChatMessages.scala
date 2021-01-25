package pl.klawoj.chat.db

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.github.plokhotnyuk.jsoniter_scala.core
import pl.klawoj.chat.domain.ChatShardEntity.BoundToParticularChat.ChatId
import pl.klawoj.chat.domain.ChatShardEntity.ChatMessage
import pl.klawoj.helpers.Ack
import pl.klawoj.helpers.GuavaFutureOpts.guavaFutureOpts
import pl.klawoj.helpers.cassandra.CassandraSession

import java.lang.{Long => JavaLong}
import java.nio.ByteBuffer
import scala.concurrent.{ExecutionContext, Future}

object ChatMessages {

  private val incomingBuffer = 10

  import CassandraSerialization._

  private val session = CassandraSession.session

  def insert(chatId: ChatId, chatMessage: ChatMessage)(implicit executionContext: ExecutionContext): Future[Ack] = {
    val bind: Seq[Object] = Seq(
      chatId.id1,
      chatId.id2,
      chatMessage.at.toEpochMilli: JavaLong,
      ByteBuffer.wrap(core.writeToArray(chatMessage)))
    insert(Queries.statementInsertNewChatMessage, bind)
  }


  def allMessagesForChat(chatId: ChatId)(implicit executionContext: ExecutionContext): Source[ChatMessage, NotUsed] =
    ChatMessages.source(Queries.messagesForChat, chatId.ids.toSeq)

  private def insert(incomingStatement: PreparedStatement, bind: Seq[Object])(implicit executionContext: ExecutionContext): Future[Ack] = {
    val statement: BoundStatement = incomingStatement.bind(bind.toArray: _*)
    session.executeAsync(statement).asScala().map(_ => Ack())
  }

  private def source(incomingStatement: PreparedStatement, bind: Seq[Object])(implicit executionContext: ExecutionContext): Source[ChatMessage, NotUsed] = {
    RowSource(incomingStatement, bind)
      .buffer(incomingBuffer, OverflowStrategy.backpressure)
      .map { row =>
        val bb = row.getBytesUnsafe("message_json")
        core.readFromArray[ChatMessage](bb.array())
      }
  }


}
