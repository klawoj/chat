package pl.klawoj.chat.http

import akka.NotUsed
import akka.stream.SourceRef
import akka.stream.scaladsl.Source
import pl.klawoj.chat.domain.{ChatQueryService, ChatShard, ChatShardEntity, Convert}
import pl.klawoj.helpers.ServiceRegistry.askHimByName
import pl.klawoj.helpers.{Ack, ServiceCallTimeout}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

trait ChatRouteFacade extends ServiceCallTimeout {

  implicit val dispatcher: ExecutionContext

  import Convert._

  def getAllUserChats(userId: String): Future[Source[OngoingChat, NotUsed]] =
    askHimByName[ChatQueryService, SourceRef[ChatShardEntity.OngoingChat]](GetAllUserChats(userId)
      .pipe(toDomain))
      .map(_.source.map(fromDomain))


  def startChat(myId: String, hisId: String): Future[OngoingChat] =
    askHimByName[ChatShard, ChatShardEntity.OngoingChat](StartChat(ChatParticipantIds(myId, hisId))
      .pipe(toDomain))
      .map(fromDomain)

  def getAllChatMessages(myId: String, hisId: String): Future[Source[ChatMessage, NotUsed]] =
    askHimByName[ChatShard, SourceRef[ChatShardEntity.ChatMessage]](GetAllMessagesInChat(ChatParticipantIds(myId, hisId))
      .pipe(toDomain))
      .map(_.source.map(fromDomain))

  def postMessageInChat(myId: String, hisId: String, message: ChatMessageContent): Future[Ack] =
    askHimByName[ChatShard, Ack](PostMessage(ChatParticipantIds(myId, hisId), message)
      .pipe(toDomain))

}
