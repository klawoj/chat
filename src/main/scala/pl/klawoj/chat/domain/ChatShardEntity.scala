package pl.klawoj.chat.domain

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, ReceiveTimeout, Stash, Status}
import akka.cluster.sharding.ShardRegion
import akka.pattern.pipe
import akka.stream.scaladsl.{Keep, Sink, Source, StreamRefs}
import akka.stream.{ActorMaterializer, Materializer}
import pl.klawoj.chat.domain.ChatShardEntity.BoundToParticularChat.ChatId
import pl.klawoj.helpers.Ack

import java.time.Instant
import scala.collection.{SortedSet, immutable}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class ChatShardEntity extends Actor with ActorLogging with Stash {

  import ChatShardEntity._

  lazy val chatId: ChatId = ChatId.fromString(self.path.name)

  implicit val dispatcher: ExecutionContext = context.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()

  override def preStart(): Unit = {
    super.preStart()
    recoverState().pipeTo(self)
  }

  override def receive: Receive = recovery

  def recovery: Receive = {
    case Loaded(ongoingChat, messages) =>
      unstashAll()
      context.setReceiveTimeout(10.minutes)
      //TODO we could configure that to tweak memory consumption
      context become operational(ongoingChat, messages)

    case failure: Status.Failure =>

      log.error(failure.cause, "Unable to recover: " + chatId)
      context.stop(self)
    case _ => stash()
  }

  def operational(chat: Option[OngoingChat], messages: List[ChatMessage]): Receive = {
    case ReceiveTimeout =>
      passivate()

    case GetAllMessagesInChat(_) =>
      sender() ! Source(messages).toMat(StreamRefs.sourceRef())(Keep.right).run()

    case StartChat(participants) =>
      val ref = sender()
      chat match {
        case Some(ongoingChat) => ref ! ongoingChat
        case None =>
          markChatOngoing(participants).map(ChatStartedPersisted) pipeTo self
          context.become(waitingForStorage(ref, chat, messages))
      }

    case PostMessage(participants, content) =>
      val ref = sender()
      chat match {
        case Some(_) =>
          val message = ChatMessage(participants.senderId, Instant.now(), content)
          postMessage(participants, message).map(_ => MessagePersisted(message)) pipeTo self
          context.become(waitingForStorage(ref, chat, messages))
        case None =>
          ref ! Status.Failure(new IllegalArgumentException("There is no such conversation"))
      }

  }

  def waitingForStorage(sender: ActorRef, chat: Option[OngoingChat], messages: List[ChatMessage]): Receive = {
    case ChatStartedPersisted(chat) =>
      unstashAll()
      sender ! chat
      context.become(operational(Some(chat), messages))
    case MessagePersisted(message) =>
      unstashAll()
      sender ! Ack()
      context.become(operational(chat.map(_.copy(lastMessage = Some(message))), message :: messages))
    case failure: Status.Failure =>
      unstashAll()
      sender ! failure
      context.become(operational(chat, messages))
    case _ => stash()
  }


  private def passivate(): Unit = {
    log.debug(s"Passivating $chatId")
    context.parent ! ShardRegion.Passivate(PoisonPill)
  }

  def getAllChatMessages(chat: ChatId): Future[Source[ChatMessage, NotUsed]] = ???

  def getSingleChatInfo(chat: ChatId): Future[Option[OngoingChat]] = ???

  def postMessage(participants: ChatOperationParticipantIds, chatMessage: ChatMessage): Future[Ack] = ???

  def markChatOngoing(participants: ChatOperationParticipantIds): Future[OngoingChat] = ???

  private def recoverState(): Future[Loaded] = {
    for {
      info <- getSingleChatInfo(chatId)
      messagesSource <- info match {
        case Some(value) => getAllChatMessages(chatId)
        case None => Future(Source.empty[ChatMessage])
      }
      messages <- messagesSource.runWith(Sink.seq[ChatMessage])
    } yield Loaded(info, messages.toList)
  }
}

object ChatShardEntity {

  private case class ChatStartedPersisted(ongoingChat: OngoingChat)

  private case class MessagePersisted(ongoingChat: ChatMessage)

  private case class Loaded(ongoingChat: Option[OngoingChat], messages: List[ChatMessage])

  case class StartChat(participants: ChatOperationParticipantIds) extends BoundToParticularChat

  case class GetAllMessagesInChat(participants: ChatOperationParticipantIds) extends BoundToParticularChat


  case class PostMessage(participants: ChatOperationParticipantIds, messageContent: ChatMessageContent) extends BoundToParticularChat

  case class OngoingChat(
                          createdAt: Instant,
                          participants: ChatOperationParticipantIds,
                          lastMessage: Option[ChatMessage]
                        )

  case class ChatMessage(senderId: String, at: Instant, content: ChatMessageContent)

  case class ChatOperationParticipantIds(senderId: String, receiverId: String)

  case class ChatMessageContent(content: String)

  sealed trait BoundToParticularChat {
    def participants: ChatOperationParticipantIds

    def chatId: ChatId = ChatId(SortedSet(participants.receiverId, participants.senderId))
  }

  object BoundToParticularChat {

    case class ChatId private(ids: SortedSet[String]) {
      require(ids.size == 2)

      override def toString: String = ids.mkString("*")
    }

    object ChatId {
      def fromString(entityKey: String): ChatId = ChatId(entityKey.split('*').to(immutable.SortedSet))
    }

  }

}
