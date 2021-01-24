package pl.klawoj.chat.domain

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import pl.klawoj.chat.domain.ChatShard.ShardMessage
import pl.klawoj.chat.domain.ChatShardEntity.BoundToParticularChat
import pl.klawoj.helpers.{ActorFactory, ServiceRegistry}

class ChatShard extends Actor {

  private val system: ActorSystem = context.system

  protected val shardsCount: Int = 16

  protected def rememberEntities = false

  private val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: BoundToParticularChat => (msg.entityKey, msg)
  }

  private val extractShardId: ShardRegion.ExtractShardId = {
    case msg: BoundToParticularChat => msg.shardRegion(shardsCount)
    case ShardRegion.StartEntity(entityKey) => ShardMessage.shardRegion(shardsCount, entityKey)
  }

  private val sharding: ActorRef = ClusterSharding(system).start(
    typeName = getClass.getSimpleName,
    entityProps = Props[ChatShardEntity],
    settings = ClusterShardingSettings(system).
      withRole("chat").
      withRememberEntities(rememberEntities)
    ,
    extractEntityId = extractEntityId,
    extractShardId = extractShardId)


  override def receive: Receive = {
    case msg => sharding forward msg
  }
}

object ChatShard extends ActorFactory[ChatShard] {

  implicit class ShardMessage(c: BoundToParticularChat) {
    def shardRegion(numberOfShards: Int): String = ShardMessage.shardRegion(numberOfShards, entityKey)

    def entityKey: String = c.chatId.toString
  }

  object ShardMessage {
    def shardRegion(numberOfShards: Int, input: String): String =
      (BigInt(input.getBytes) mod numberOfShards).toString
  }

  override val actorName: String = "ChatShard"

  override def postCreate(actorRef: ActorRef): Unit = {
    ServiceRegistry.register[ChatShard](actorRef)
  }
}
