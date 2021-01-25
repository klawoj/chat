package pl.klawoj.chat.http

import java.time.Instant

sealed trait ChatProtocol

case class GetAllUserChats(userId: String) extends ChatProtocol


case class StartChat(participants: ChatParticipantIds) extends ChatProtocol

case class GetAllMessagesInChat(participants: ChatParticipantIds) extends ChatProtocol


case class PostMessage(participants: ChatParticipantIds, messageContent: ChatMessageContent) extends ChatProtocol

case class OngoingChat(
                        createdAt: Instant,
                        participants: Seq[Participant],
                        lastMessage: Option[ChatMessage]
                      )

case class Participant(id: String, name: String)


case class ChatMessage(senderId: String, at: Instant, content: ChatMessageContent)

case class ChatMessageContent(text: String)

case class ChatParticipantIds(senderId: String, receiverId: String)


