package pl.klawoj.helpers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Source, StreamRefs}
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{TestActor, TestProbe}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.reflect.runtime.universe._
import scala.util.chaining.scalaUtilChainingOps

trait ServiceRegistryMockHelper {

  implicit val system: ActorSystem

  case class AutoPilotContext(sender: ActorRef, message: Any)

  def registerAutoPilotProbe[T: TypeTag](pilot: PartialFunction[AutoPilotContext, AutoPilot]): TestProbe = {
    val probe = TestProbe()

    probe.setAutoPilot((sender: ActorRef, msg: Any) => {
      val autoPilotContext = AutoPilotContext(sender, msg)
      pilot.lift(autoPilotContext).getOrElse(TestActor.NoAutoPilot)
    })

    ServiceRegistry register[T] probe.ref
    probe
  }

  def mockResponseFunction[A, B](request: A, response: B): PartialFunction[AutoPilotContext, akka.testkit.TestActor.AutoPilot] = {
    case AutoPilotContext(sender, msg) if msg == request =>
      sender ! response
      TestActor.KeepRunning
  }

  def mockStreamResponseFunction[A, B](request: A, response: immutable.Iterable[B])(implicit materializer: Materializer): PartialFunction[AutoPilotContext, akka.testkit.TestActor.AutoPilot] = {
    case AutoPilotContext(sender, msg) if msg == request =>
      sender ! Source(response)
        .toMat(StreamRefs.sourceRef())(Keep.right)
        .run()
        .pipe(Await.result(_, 3.seconds)).source
      TestActor.KeepRunning
  }
}
