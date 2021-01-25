package pl.klawoj.chat.domain


import io.bfil.automapper._
import pl.klawoj.chat.http._

object Convert extends ConvertToDomain with ConvertFromDomain

trait ConvertToDomain {
  def toDomain(msg: StartChat): ChatShardEntity.StartChat =
    automap(msg).to[ChatShardEntity.StartChat]

  def toDomain(msg: GetAllUserChats): ChatQueryService.GetAllUserChats =
    automap(msg).to[ChatQueryService.GetAllUserChats]

  def toDomain(msg: GetAllMessagesInChat): ChatShardEntity.GetAllMessagesInChat =
    automap(msg).to[ChatShardEntity.GetAllMessagesInChat]

  def toDomain(msg: PostMessage): ChatShardEntity.PostMessage =
    automap(msg).to[ChatShardEntity.PostMessage]

  def toDomain(msg: ChatMessageContent): ChatShardEntity.ChatMessageContent =
    automap(msg).to[ChatShardEntity.ChatMessageContent]

  def toDomain(msg: ChatMessage): ChatShardEntity.ChatMessage =
    automap(msg).to[ChatShardEntity.ChatMessage]

  def toDomain(msg: OngoingChat): ChatShardEntity.OngoingChat = {
    automap(msg).to[ChatShardEntity.OngoingChat]
  }
}

trait ConvertFromDomain {
  def fromDomain(msg: ChatShardEntity.OngoingChat): OngoingChat = {
    automap(msg).to[OngoingChat]
  }

  def fromDomain(msg: ChatShardEntity.ChatMessage): ChatMessage = {
    automap(msg).to[ChatMessage]
  }
}
