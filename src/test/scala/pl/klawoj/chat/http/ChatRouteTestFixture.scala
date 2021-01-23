package pl.klawoj.chat.http

import akka.http.scaladsl.testkit.RouteTest
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pl.klawoj.chat.domain.{ChatQueryService, ChatShard}
import pl.klawoj.chat.http.ChatProtocol._
import pl.klawoj.helpers.ServiceRegistryMockHelper

trait ChatRouteTestFixture extends ScalaCheckPropertyChecks with ServiceRegistryMockHelper with ChatGen {

  this: RouteTest =>


  def havingGetUserChatsServiceResponseMocked(inner: (String, Seq[OngoingChat]) => Unit): Unit = {
    forAll(genId(), Gen.listOfN[OngoingChat](10, genOngoingChat()).map(_.toSeq)) { (id, chats) =>
      registerAutoPilotProbe[ChatQueryService](mockStreamResponseFunction(ListAllUserChats(id), chats))
      inner(id, chats)
    }
  }

  def havingStartChatServiceResponseMocked(inner: (ChatParticipants, OngoingChat) => Unit): Unit = {
    forAll(genParticipants(), genOngoingChat()) { (participants, chat) =>
      registerAutoPilotProbe[ChatShard](mockResponseFunction(StartChat(participants), chat))
      inner(participants, chat)
    }
  }

  def havingGetAllChatMessagesResponseMocked(inner: (ChatParticipants, Seq[ChatMessage]) => Unit): Unit = {
    forAll(genParticipants(), Gen.listOfN[ChatMessage](10, genChatMessage()).map(_.toSeq)) { (participants, msgs) =>
      registerAutoPilotProbe[ChatShard](mockStreamResponseFunction(GetAllChatMessages(participants), msgs))
      inner(participants, msgs)
    }
  }

  def havingPostMessageResponseMocked(inner: (PostMessage) => Unit): Unit = {
    forAll(genPostMessage()) { postMessage =>
      registerAutoPilotProbe[ChatShard](mockResponseFunction(postMessage, Ack()))
      inner(postMessage)
    }
  }


}
