package pl.klawoj.chat.db

import akka.NotUsed
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import akka.stream.scaladsl.Source
import com.datastax.driver.core.{PreparedStatement, Row}
import pl.klawoj.helpers.cassandra.CassandraSession

object RowSource {

  val incomingFetchSize: Int = 1000

  def apply(incomingStatement: PreparedStatement, bind: Seq[Object]): Source[Row, NotUsed] = {
    val statement = incomingStatement
      .bind(bind.toArray: _*)
      .setFetchSize(incomingFetchSize)

    CassandraSource(statement)(CassandraSession.session)
  }
}