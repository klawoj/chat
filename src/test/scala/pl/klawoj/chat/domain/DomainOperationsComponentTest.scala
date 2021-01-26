package pl.klawoj.chat.domain

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer, SourceRef}
import akka.testkit.{ImplicitSender, TestKit}
import org.cassandraunit.CQLDataLoader
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.scalatest
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.SpanSugar._
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AsyncWordSpecLike
import org.scalatest.{Assertion, BeforeAndAfterAll}
import pl.klawoj.chat.domain.ChatShardEntity._
import pl.klawoj.helpers.cassandra.{CassandraConfig, CassandraSession}
import pl.klawoj.helpers.{Ack, AutoShutdownTestKit, DockerTestFixture}

import java.time.Instant
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class DomainOperationsComponentTest
  extends TestKit(ActorSystem("chat"))
    with AsyncWordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with AutoShutdownTestKit
    with ImplicitSender with DockerTestFixture {

  private implicit val dispatcher: ExecutionContextExecutor = system.dispatcher
  private implicit val materializer: Materializer = ActorMaterializer()
  private val cassandraConfig = CassandraConfig.load()

  private implicit val log: LoggingAdapter = Logging(system.eventStream, this.getClass.getCanonicalName)

  private val CassandraImage = "bitnami/cassandra:latest"
  private val CassandraWaitDuration = 120.seconds
  private val CassandraHost = cassandraConfig.seeds.headOption.getOrElse(throw new IllegalArgumentException("cassandra host could not be found"))
  private val CassandraPort = cassandraConfig.port
  private val CassandraEnv = List("CASSANDRA_AUTHENTICATOR=PasswordAuthenticator")
  private val CassandraSchemaName = cassandraConfig.schemaName
  private val CassandraCqlFile = s"${cassandraConfig.schemaName}.cql"
  private val CassandraContainerName = "/" + CassandraSchemaName

  val ignoredInstant: Instant = Instant.now()

  val asyncSession = CassandraSession.asyncSession.map { session =>
    val dataLoader = new CQLDataLoader(session)
    dataLoader.load(new ClassPathCQLDataSet(CassandraCqlFile, CassandraSchemaName))
    session
  }
  val query = ChatQueryService.actor
  private val shard: Future[ActorRef] = asyncSession.map(_ => ChatShard.actor)

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    removeContainer(CassandraContainerName)
    createContainer(CassandraImage, CassandraContainerName, CassandraPort, CassandraEnv)
    startContainer(CassandraContainerName)
    Await.ready(asyncSession, CassandraWaitDuration)
    waitForPort()
  }

  protected override def afterAll(): Unit = {
    CassandraSession.cluster.close()
    super.afterAll()
    removeContainer(CassandraContainerName)
  }

  private def waitForPort(): Unit = {
    val timeout = Eventually.scaled(Span(30, Seconds))
    val interval = Eventually.scaled(Span(1, Seconds))
    implicit val patienceConfig: scalatest.concurrent.Eventually.PatienceConfig = PatienceConfig(timeout, interval)

    Eventually.eventually {
      log.info(s"Checking $CassandraHost:$CassandraPort")
      val socket = new java.net.Socket(CassandraHost, CassandraPort)
      socket.close()
    }
  }

  "Chat app" must {
    "enable users exchange messages and list active conversations " when {
      "they create the conversation first" in {
        shard.flatMap { ref =>

          validateMessagesInChat(ref, ChatParticipantIds("1", "2"), Seq.empty)

          ref ! ChatShardEntity.StartChat(ChatParticipantIds("1", "2"))
          expectMsgType[OngoingChat].participants shouldBe Seq(Participant("1", "John"), Participant("2", "Adam"))

          ref ! ChatShardEntity.PostMessage(ChatParticipantIds("1", "2"), ChatMessageContent("First message!"))
          expectMsgType[Ack]

          validateMessagesInChat(ref, ChatParticipantIds("1", "2"), Seq(("1", "First message!")))

          ref ! ChatShardEntity.PostMessage(ChatParticipantIds("2", "1"), ChatMessageContent("Response message!"))
          expectMsgType[Ack]

          validateMessagesInChat(ref, ChatParticipantIds("2", "1"), Seq(("2", "Response message!"), ("1", "First message!")))

          validateMessagesInChat(ref, ChatParticipantIds("1", "2"), Seq(("2", "Response message!"), ("1", "First message!")))

          validateConversationsForUser(query, "1", Seq(
            OngoingChat(ignoredInstant,
              Seq(Participant("1", "John"), Participant("2", "Adam")),
              Some(ChatMessage("2", ignoredInstant, ChatMessageContent("Response message!")))
            )))

          validateConversationsForUser(query, "2", Seq(
            OngoingChat(ignoredInstant,
              Seq(Participant("1", "John"), Participant("2", "Adam")),
              Some(ChatMessage("2", ignoredInstant, ChatMessageContent("Response message!")))
            )))

          ref ! ChatShardEntity.StartChat(ChatParticipantIds("3", "1"))
          expectMsgType[OngoingChat].participants shouldBe Seq(Participant("3", "Susan"), Participant("1", "John"))

          validateConversationsForUser(query, "1", Seq(
            OngoingChat(ignoredInstant,
              Seq(Participant("3", "Susan"), Participant("1", "John")),
              None
            ),
            OngoingChat(ignoredInstant,
              Seq(Participant("1", "John"), Participant("2", "Adam")),
              Some(ChatMessage("2", ignoredInstant, ChatMessageContent("Response message!")))
            )))

          validateConversationsForUser(query, "2", Seq(
            OngoingChat(ignoredInstant,
              Seq(Participant("1", "John"), Participant("2", "Adam")),
              Some(ChatMessage("2", ignoredInstant, ChatMessageContent("Response message!")))
            )))

          validateConversationsForUser(query, "3", Seq(
            OngoingChat(ignoredInstant,
              Seq(Participant("3", "Susan"), Participant("1", "John")),
              None
            )))

        }
      }
    }
  }

  def validateMessagesInChat(ref: ActorRef, chatParticipantIds: ChatParticipantIds, messages: Seq[(String, String)]): Assertion = {
    ref ! ChatShardEntity.GetAllMessagesInChat(chatParticipantIds)
    val msgList = expectMsgType[SourceRef[ChatMessage]].toSeq()
    msgList.map(el => (el.senderId, el.content.text)) shouldBe messages
  }

  def validateConversationsForUser(ref: ActorRef, userId: String, messages: Seq[OngoingChat]): Assertion = {
    import com.softwaremill.quicklens._
    ref ! ChatQueryService.GetAllUserChats(userId)
    val msgList = expectMsgType[SourceRef[OngoingChat]].toSeq()
    msgList
      .modify(_.each.createdAt).setTo(ignoredInstant)
      .modify(_.each.lastMessage.each.at).setTo(ignoredInstant) shouldBe messages
  }


  implicit class futureAwait[T](future: Future[T]) {
    def await(): T = Await.result(future, 3.seconds)
  }

  implicit class sourceRefToSeq[T](sourceRef: SourceRef[T])(implicit materializer: Materializer) {
    def toSeq(): Seq[T] = sourceRef.source.runWith(Sink.seq).await()
  }

}