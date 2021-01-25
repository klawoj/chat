package pl.klawoj.chat.domain

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import akka.stream.scaladsl.{Keep, StreamRefs}
import akka.stream.{ActorMaterializer, Materializer}
import pl.klawoj.chat.db.OngoingChatRepository
import pl.klawoj.chat.domain.ChatQueryService.GetAllUserChats
import pl.klawoj.helpers.{ActorFactory, ServiceRegistry}

import scala.concurrent.ExecutionContext

class ChatQueryService extends Actor {

  implicit val materializer: Materializer = ActorMaterializer()
  implicit val dispatcher: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case GetAllUserChats(userId) =>
      val ref = sender()
      OngoingChatRepository.forUser(userId).toMat(StreamRefs.sourceRef())(Keep.right).run().pipeTo(ref)
  }
}

object ChatQueryService extends ActorFactory[ChatQueryService] {

  case class GetAllUserChats(userId: String)

  override val actorName: String = "ChatQueryService"

  override def postCreate(actorRef: ActorRef): Unit = {
    ServiceRegistry.register[ChatQueryService](actorRef)
  }
}
