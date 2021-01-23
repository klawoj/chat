package pl.klawoj.helpers

import akka.actor.{Actor, ActorContext}
import akka.http.scaladsl.server.Route
import akka.testkit.TestActorRef
import org.scalatest.Suite
import pl.klawoj.chat.http.ChatRoute

trait ChatRouteTestConfig extends Suite with AutoShutdownTestKit {
  def withHttpConfig[T](fn: HttpConfig => T): T =
    fn(HttpConfig.load())

  def withRoute[T](fn: Route => T)(implicit httpConfig: HttpConfig): T = {
    def mainRoute(implicit httpConfig: HttpConfig): Route = {
      val testActor = TestActorRef[TestActor]
      implicit val context: ActorContext = testActor.underlyingActor.context
      ChatRoute.route
    }

    val sut = Route.seal(mainRoute)
    fn(sut)
  }

}

class TestActor extends Actor with ActorEmptyReceive