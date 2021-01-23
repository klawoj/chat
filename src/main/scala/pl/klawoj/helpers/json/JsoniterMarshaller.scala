package pl.klawoj.helpers.json

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, MessageEntity}
import akka.http.scaladsl.{marshalling, unmarshalling}
import akka.util.ByteString
import com.github.plokhotnyuk.jsoniter_scala.core
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, WriterConfig}

trait JsoniterMarshaller
  extends Marshaller {

  private val writerBufferSize = 64000
  private val writerConfig = WriterConfig.withPreferredBufSize(writerBufferSize)

  trait JsoniterMarshallable[T <: AnyRef] extends Marshallable[T] {
    implicit val jsonValueCodec: JsonValueCodec[T]

    override def read(json: String): T = readCodec(json)

    override def write(o: T): String = writeCodecString(o)

    override def writeToArray(o: T): Array[Byte] = writeCodec(o)
  }

  implicit def marshaller[T <: AnyRef : Marshallable]: marshalling.Marshaller[T, MessageEntity] =
    marshallerFor[T]

  private def marshallerFor[T <: AnyRef](implicit marshallable: Marshallable[T]): marshalling.Marshaller[T, MessageEntity] =
    marshalling.Marshaller
      .byteArrayMarshaller(`application/json`)
      .compose(x => marshallable.writeToArray(x))

  implicit def unmarshaller[T <: AnyRef : Marshallable]: unmarshalling.Unmarshaller[HttpEntity, T] =
    unmarshallerFor[T]

  private def unmarshallerFor[T <: AnyRef](implicit marshallable: Marshallable[T]): unmarshalling.Unmarshaller[HttpEntity, T] =
    unmarshalling.Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw unmarshalling.Unmarshaller.NoContentException
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }
      .map(s => marshallable.read(s))

  private def writeCodec[T](o: T)(implicit codec: JsonValueCodec[T]): Array[Byte] =
    core.writeToArray[T](o, writerConfig)

  private def writeCodecString[T](o: T)(implicit codec: JsonValueCodec[T]): String =
    new String(writeCodec[T](o))

  private def readCodec[T](o: String)(implicit codec: JsonValueCodec[T]): T =
    core.readFromArray[T](o.getBytes)
}