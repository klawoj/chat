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
    askHimByName[ChatQueryService, SourceRef[OngoingChat]](GetAllUserChats(userId)).map(_.source)


  def startChat(myId: String, hisId: String): Future[OngoingChat] =
    askHimByName[ChatShard, ChatShardEntity.OngoingChat](StartChat(ChatOperationParticipantIds(myId, hisId))
      .pipe(toDomain))
      .map(fromDomain)

  def getAllChatMessages(myId: String, hisId: String): Future[Source[ChatMessage, NotUsed]] =
    askHimByName[ChatShard, SourceRef[ChatShardEntity.ChatMessage]](GetAllMessagesInChat(ChatOperationParticipantIds(myId, hisId))
      .pipe(toDomain))
      .map(_.source.map(fromDomain))

  def postMessageInChat(myId: String, hisId: String, message: ChatMessageContent): Future[Ack] =
    askHimByName[ChatShard, Ack](PostMessage(ChatOperationParticipantIds(myId, hisId), message)
      .pipe(toDomain))

}
