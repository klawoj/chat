package pl.klawoj.chat.http

import org.scalacheck.Gen

import java.time.Instant

trait ChatGen {

  def genInstant(): Gen[Instant] = Gen.delay(Gen.const(Instant.now()))

  def genId(): Gen[String] = Gen.uuid.map(_.toString)

  def genParticipantIds(): Gen[ChatParticipantIds] = for {
    id1 <- genId()
    id2 <- genId().suchThat(_ != id1)
  } yield ChatParticipantIds(id1, id2)

  def genParticipant(): Gen[Participant] = for {
    id <- genId()
    name <- Gen.alphaNumStr
  } yield Participant(id, name)


  def genPostMessage(): Gen[PostMessage] = for {
    p <- genParticipantIds()
    msg <- genChatMessageContent()
  } yield PostMessage(p, msg)

  def genChatMessageContent(): Gen[ChatMessageContent] =
    Gen.alphaNumStr.map(ChatMessageContent)

  def genChatMessage(): Gen[ChatMessage] = for {
    by <- genId()
    at <- Gen.delay(Gen.const(Instant.now()))
    content <- genChatMessageContent()
  } yield ChatMessage(by, at, content)

  def genOngoingChat(): Gen[OngoingChat] = for {
    createdAt <- genInstant()
    participants <- Gen.listOfN(2, genParticipant())
    lastMessage <- Gen.option(genChatMessage())
  } yield OngoingChat(createdAt, participants, lastMessage)
}
