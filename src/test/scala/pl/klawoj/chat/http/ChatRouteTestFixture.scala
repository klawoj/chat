package pl.klawoj.chat.http

import akka.http.scaladsl.testkit.RouteTest
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pl.klawoj.chat.domain.{ChatQueryService, ChatShard}
import pl.klawoj.helpers.{Ack, ServiceRegistryMockHelper}

trait ChatRouteTestFixture extends ScalaCheckPropertyChecks with ServiceRegistryMockHelper with ChatGen {

  this: RouteTest =>

  import pl.klawoj.chat.domain.Convert._

  def havingGetUserChatsServiceResponseMocked(inner: (String, Seq[OngoingChat]) => Unit): Unit = {
    forAll(genId(), Gen.listOfN[OngoingChat](10, genOngoingChat()).map(_.toSeq)) { (id, chats) =>
      registerAutoPilotProbe[ChatQueryService](mockStreamResponseFunction(GetAllUserChats(id), chats))
      inner(id, chats)
    }
  }

  def havingStartChatServiceResponseMocked(inner: (ChatOperationParticipantIds, OngoingChat) => Unit): Unit = {
    forAll(genParticipants(), genOngoingChat()) { (participants, chat: OngoingChat) =>
      registerAutoPilotProbe[ChatShard](mockResponseFunction(toDomain(StartChat(participants)), toDomain(chat)))
      inner(participants, chat)
    }
  }

  def havingGetAllChatMessagesResponseMocked(inner: (ChatOperationParticipantIds, Seq[ChatMessage]) => Unit): Unit = {
    forAll(genParticipants(), Gen.listOfN[ChatMessage](10, genChatMessage()).map(_.toSeq)) { (participants, msgs) =>
      registerAutoPilotProbe[ChatShard](mockStreamResponseFunction(toDomain(GetAllMessagesInChat(participants)), msgs.map(toDomain)))
      inner(participants, msgs)
    }
  }

  def havingPostMessageResponseMocked(inner: (PostMessage) => Unit): Unit = {
    forAll(genPostMessage()) { postMessage =>
      registerAutoPilotProbe[ChatShard](mockResponseFunction(toDomain(postMessage), Ack()))
      inner(postMessage)
    }
  }


}
