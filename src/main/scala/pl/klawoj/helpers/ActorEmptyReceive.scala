package pl.klawoj.helpers

import akka.actor.Actor

trait ActorEmptyReceive extends Actor {
  override def receive: Receive = {
    case _ =>
  }
}
