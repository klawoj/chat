package pl.klawoj.chat.db

import pl.klawoj.chat.domain.ChatShardEntity.{ChatParticipantIds, Participant}

import scala.concurrent.{ExecutionContext, Future}

trait UserService {

  implicit val dispatcher: ExecutionContext

  def getUserById(id: String): Future[Participant] =
    Future.successful(Seq(
      Participant("1", "John"),
      Participant("2", "Adam"),
      Participant("3", "Susan"),
    ).groupBy(_.id).view.mapValues(_.head).getOrElse(id, throw new IllegalArgumentException("User not found")))

  def getParticipants(participantIds: ChatParticipantIds): Future[Seq[Participant]] = {
    Future.sequence(Seq(
      getUserById(participantIds.senderId),
      getUserById(participantIds.receiverId)
    ))
  }

}
