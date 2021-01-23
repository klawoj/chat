package pl.klawoj.helpers

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

class HttpEndpoint(route: Route, config: HttpConfig)
  extends Actor
    with ActorLogging
    with ActorEmptyReceive {

  private implicit val system: ActorSystem = context.system
  private implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  Http().bindAndHandle(route, config.bindingHost, config.bindingPort)
}

object HttpEndpoint extends ActorFactory[HttpEndpoint] {
  override val actorName: String = "HttpEndpoint"
}
