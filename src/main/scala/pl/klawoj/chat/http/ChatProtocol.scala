package pl.klawoj.chat.http

import java.time.Instant

object ChatProtocol {

  case class ListAllUserChats(userId: String)


  case class StartChat(participants: ChatParticipants) extends BetweenChatParticipants

  case class GetAllChatMessages(participants: ChatParticipants) extends BetweenChatParticipants

  object GetAllChatMessages {

  }

  case class PostMessage(participants: ChatParticipants, chatMessage: ChatMessage) extends BetweenChatParticipants

  case class OngoingChat(
                          participants: ChatParticipants,
                          lastMessage: Option[ChatMessage]
                        ) extends BetweenChatParticipants

  case class ChatMessage(at: Instant, content: String)

  case class Ack()

  case class ChatParticipants(id1: String, id2: String)

  trait BetweenChatParticipants {
    def participants: ChatParticipants
  }

}
