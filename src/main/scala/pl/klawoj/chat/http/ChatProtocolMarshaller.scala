package pl.klawoj.chat.http

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import pl.klawoj.helpers.json.JsoniterMarshaller

object ChatProtocolMarshaller extends JsoniterMarshaller {

  implicit val listAllUserChatsMarshaller: JsoniterMarshallable[GetAllUserChats] = new JsoniterMarshallable[GetAllUserChats] {
    override implicit val jsonValueCodec: JsonValueCodec[GetAllUserChats] = make(CodecMakerConfig)
  }

  implicit val outgoingChatMarshaller: JsoniterMarshallable[OngoingChat] = new JsoniterMarshallable[OngoingChat] {
    override implicit val jsonValueCodec: JsonValueCodec[OngoingChat] = make(CodecMakerConfig)
  }

  implicit val outgoingChatSeqMarshaller: JsoniterMarshallable[Seq[OngoingChat]] = new JsoniterMarshallable[Seq[OngoingChat]] {
    override implicit val jsonValueCodec: JsonValueCodec[Seq[OngoingChat]] = make(CodecMakerConfig)
  }

  implicit val chatMessageContentMarshaller: JsoniterMarshallable[ChatMessageContent] = new JsoniterMarshallable[ChatMessageContent] {
    override implicit val jsonValueCodec: JsonValueCodec[ChatMessageContent] = make(CodecMakerConfig)
  }

  implicit val chatMessageMarshaller: JsoniterMarshallable[ChatMessage] = new JsoniterMarshallable[ChatMessage] {
    override implicit val jsonValueCodec: JsonValueCodec[ChatMessage] = make(CodecMakerConfig)
  }

  implicit val chatMessageSeqMarshaller: JsoniterMarshallable[Seq[ChatMessage]] = new JsoniterMarshallable[Seq[ChatMessage]] {
    override implicit val jsonValueCodec: JsonValueCodec[Seq[ChatMessage]] = make(CodecMakerConfig)
  }
}
