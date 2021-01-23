package pl.klawoj.chat.http

import akka.NotUsed
import akka.stream.scaladsl.Source
import pl.klawoj.chat.domain.{ChatQueryService, ChatShard}
import pl.klawoj.chat.http.ChatProtocol._
import pl.klawoj.helpers.ServiceCallTimeout
import pl.klawoj.helpers.ServiceRegistry.askHim

import scala.concurrent.Future

trait ChatRouteFacade extends ServiceCallTimeout {

  def getAllUserChats(userId: String): Future[Source[OngoingChat, NotUsed]] = {
    askHim[ChatQueryService, Source[OngoingChat, NotUsed]](ListAllUserChats(userId))
  }

  def startChat(myId: String, hisId: String): Future[OngoingChat] =
    askHim[ChatShard, OngoingChat](StartChat(ChatParticipants(myId, hisId)))

  def getAllChatMessages(myId: String, hisId: String): Future[Source[ChatMessage, NotUsed]] =
    askHim[ChatShard, Source[ChatMessage, NotUsed]](GetAllChatMessages(ChatParticipants(myId, hisId)))

  def postMessageInChat(myId: String, hisId: String, message: ChatMessage): Future[Ack] =
    askHim[ChatShard, Ack](PostMessage(ChatParticipants(myId, hisId), message))

}
