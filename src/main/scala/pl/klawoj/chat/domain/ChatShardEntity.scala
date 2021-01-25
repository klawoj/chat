package pl.klawoj.chat.domain

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, ReceiveTimeout, Stash, Status}
import akka.cluster.sharding.ShardRegion
import akka.event.Logging.InfoLevel
import akka.event.LoggingReceive
import akka.pattern.pipe
import akka.stream.scaladsl.{Keep, Sink, Source, StreamRefs}
import akka.stream.{ActorMaterializer, Materializer}
import pl.klawoj.chat.db.{ChatMessageRepository, OngoingChatRepository, UserService}
import pl.klawoj.chat.domain.ChatShardEntity.BoundToParticularChat.ChatId
import pl.klawoj.helpers.Ack

import java.time.Instant
import scala.collection.{SortedSet, immutable}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}


//TODO probably a PersistentActor would be a more elegant solution here
class ChatShardEntity extends Actor with ActorLogging with Stash with UserService {

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
      //TODO we could configure that to tweak memory consumption (ReceiveTimeout passivates the entity)
      context become operational(ongoingChat, messages)
    case failure: Status.Failure =>

      log.error(failure.cause, "Unable to recover: " + chatId)
      context.stop(self)
    case _ => stash()
  }

  def operational(chat: Option[OngoingChat], messages: List[ChatMessage]): Receive = LoggingReceive(InfoLevel) {
    case ReceiveTimeout =>
      passivate()

    case GetAllMessagesInChat(_) =>
      val ref = sender()
      Source(messages).toMat(StreamRefs.sourceRef())(Keep.right).run() pipeTo ref

    case StartChat(participantIds) =>
      val ref = sender()
      chat match {
        case Some(ongoingChat) => ref ! ongoingChat
        case None =>
          getParticipants(participantIds)
            .map { participants =>
              OngoingChat(
                createdAt = Instant.now(), participants = participants, lastMessage = None
              )
            }
            .flatMap(persistChatState)
            .map(OngoingChatPersisted) pipeTo self
          context.become(waitingForStorage(ref, chat, messages))
      }

    case PostMessage(participants, content) =>
      val ref = sender()
      chat match {
        case Some(chatState) =>
          val message = ChatMessage(participants.senderId, Instant.now(), content)
          val newChatState = chatState.withNewMessage(message)
          persistNewMessage(message, newChatState).map(MessagePersisted.tupled) pipeTo self
          context.become(waitingForStorage(ref, chat, messages))
        case None =>
          ref ! Status.Failure(new NoSuchElementException("There is no such conversation"))
      }
  }

  def waitingForStorage(sender: ActorRef, chat: Option[OngoingChat], messages: List[ChatMessage]): Receive = LoggingReceive(InfoLevel) {
    case OngoingChatPersisted(chat) =>
      unstashAll()
      sender ! chat
      context.become(operational(Some(chat), messages))
    case MessagePersisted(chat, message) =>
      unstashAll()
      sender ! Ack()
      context.become(operational(Some(chat), message :: messages))
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

  def getAllChatMessages(): Source[ChatMessage, NotUsed] = {
    ChatMessageRepository.allMessagesForChat(chatId)
  }

  def getChatInfo(): Future[Option[OngoingChat]] = {
    OngoingChatRepository.forChat(chatId).limit(1).toMat(Sink.headOption)(Keep.right).run()
  }

  def persistNewMessage(message: ChatMessage, chat: OngoingChat): Future[(OngoingChat, ChatMessage)] = {
    persistChatState(chat).zip(persistChatMessage(message))
  }

  def persistChatMessage(chatMessage: ChatMessage): Future[ChatMessage] = {
    ChatMessageRepository.insert(chatId, chatMessage).map(_ => chatMessage)
  }

  def persistChatState(chat: OngoingChat): Future[OngoingChat] = {
    Future.sequence(Seq(
      OngoingChatRepository.insertForChat(chatId, chat),
      OngoingChatRepository.insertForUser(chatId.id1, chat),
      OngoingChatRepository.insertForUser(chatId.id2, chat)
    )).map(_ => chat)
  }

  private def recoverState(): Future[Loaded] = {
    for {
      info <- getChatInfo()
      messagesSource = info match {
        case Some(value) => getAllChatMessages()
        case None => Source.empty[ChatMessage]
      }
      messages <- messagesSource.runWith(Sink.seq[ChatMessage])
    } yield Loaded(info, messages.toList)
  }
}

object ChatShardEntity {

  private case class OngoingChatPersisted(chat: OngoingChat)

  private case class MessagePersisted(ongoingChat: OngoingChat, message: ChatMessage)

  private case class Loaded(ongoingChat: Option[OngoingChat], messages: List[ChatMessage])

  case class StartChat(participants: ChatParticipantIds) extends BoundToParticularChat

  case class GetAllMessagesInChat(participants: ChatParticipantIds) extends BoundToParticularChat


  case class PostMessage(participants: ChatParticipantIds, messageContent: ChatMessageContent) extends BoundToParticularChat

  case class OngoingChat(
                          createdAt: Instant,
                          participants: Seq[Participant],
                          lastMessage: Option[ChatMessage]
                        ) {

    def withNewMessage(chatMessage: ChatMessage): OngoingChat = {
      copy(lastMessage = Some(chatMessage))
    }

  }

  case class ChatMessage(senderId: String, at: Instant, content: ChatMessageContent)

  case class Participant(id: String, name: String)

  case class ChatParticipantIds(senderId: String, receiverId: String)

  case class ChatMessageContent(text: String)

  sealed trait BoundToParticularChat {
    def participants: ChatParticipantIds

    def chatId: ChatId = ChatId(SortedSet(participants.receiverId, participants.senderId))
  }

  object BoundToParticularChat {

    case class ChatId private(ids: SortedSet[String]) {
      require(ids.size == 2)

      override def toString: String = ids.mkString("*")

      def id1: String = ids.head

      def id2: String = ids.toSeq(1)
    }

    object ChatId {
      def fromString(entityKey: String): ChatId = ChatId(entityKey.split('*').to(immutable.SortedSet))
    }

  }

}
