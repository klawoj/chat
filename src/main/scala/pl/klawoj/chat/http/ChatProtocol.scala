package pl.klawoj.chat.http

import java.time.Instant

sealed trait ChatProtocol

case class GetAllUserChats(userId: String) extends ChatProtocol


case class StartChat(participants: ChatOperationParticipantIds) extends ChatProtocol

case class GetAllMessagesInChat(participants: ChatOperationParticipantIds) extends ChatProtocol


case class PostMessage(participants: ChatOperationParticipantIds, messageContent: ChatMessageContent) extends ChatProtocol

case class OngoingChat(
                        createdAt: Instant,
                        participants: ChatOperationParticipantIds,
                        lastMessage: Option[ChatMessage]
                      )

case class ChatMessage(senderId: String, at: Instant, content: ChatMessageContent)

case class ChatMessageContent(content: String)

case class ChatOperationParticipantIds(senderId: String, receiverId: String)

