package pl.klawoj.chat.db

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import pl.klawoj.chat.domain.ChatShardEntity.{ChatMessage, OngoingChat}

private[db] object CassandraSerialization {

  //TODO model used for serialization should be independent from domain model
  //TODO tests should be added for bidirectional conversion between the model and db json. By changing the model by accident we would not be able to read existing data!
  //TODO we should solve schema evolution somehow (for ex. versioning serializers and keeping the version in cassandra row)

  implicit val ongoingChatCodec: JsonValueCodec[OngoingChat] = make(CodecMakerConfig)
  implicit val chatMessageCodec: JsonValueCodec[ChatMessage] = make(CodecMakerConfig)

}
