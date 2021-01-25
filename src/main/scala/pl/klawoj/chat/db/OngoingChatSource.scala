package pl.klawoj.chat.db

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.datastax.driver.core.PreparedStatement
import com.github.plokhotnyuk.jsoniter_scala.core
import pl.klawoj.chat.domain.ChatShardEntity.OngoingChat

import scala.concurrent.ExecutionContext

object OngoingChatSource {

  val incomingBuffer = 10

  import CassandraSerialization._

  def apply(incomingStatement: PreparedStatement, bind: Seq[Object])(implicit executionContext: ExecutionContext): Source[OngoingChat, NotUsed] = {
    RowSource(incomingStatement, bind)
      .buffer(incomingBuffer, OverflowStrategy.backpressure)
      .map { row =>
        val bb = row.getBytesUnsafe("chat_data_json")
        core.readFromArray[OngoingChat](bb.array())
      }
  }
}

