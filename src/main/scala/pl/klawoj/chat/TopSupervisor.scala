package pl.klawoj.chat

import akka.actor.{Actor, ActorLogging}
import pl.klawoj.chat.http.ChatRoute
import pl.klawoj.helpers.{ActorEmptyReceive, ActorFactory, HttpConfig, HttpEndpoint}

class TopSupervisor extends Actor with ActorLogging with ActorEmptyReceive {

  private implicit val httpConfig: HttpConfig = HttpConfig.load()

  HttpEndpoint.actor(ChatRoute.route, httpConfig)
}

object TopSupervisor extends ActorFactory[TopSupervisor] {
  override val actorName: String = "top"
}
