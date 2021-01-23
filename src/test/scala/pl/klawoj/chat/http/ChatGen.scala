package pl.klawoj.chat.http

import org.scalacheck.Gen
import pl.klawoj.chat.http.ChatProtocol.{ChatMessage, ChatParticipants, OngoingChat, PostMessage}

import java.time.Instant

trait ChatGen {

  def genId(): Gen[String] = Gen.uuid.map(_.toString)

  def genParticipants(): Gen[ChatParticipants] = for {
    id1 <- genId()
    id2 <- genId()
  } yield ChatParticipants(id1, id2)

  def genPostMessage(): Gen[PostMessage] = for {
    p <- genParticipants()
    msg <- genChatMessage()
  } yield PostMessage(p, msg)

  def genChatMessage(): Gen[ChatMessage] = for {
    at <- Gen.delay(Gen.const(Instant.now()))
    content <- Gen.alphaNumStr
  } yield ChatMessage(at, content)

  def genOngoingChat(): Gen[OngoingChat] = for {
    participants <- genParticipants()
    lastMessage <- Gen.option(genChatMessage())
  } yield OngoingChat(participants, lastMessage)
}
