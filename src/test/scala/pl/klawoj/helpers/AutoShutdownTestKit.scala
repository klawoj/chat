package pl.klawoj.helpers

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.duration.DurationInt

trait AutoShutdownTestKit
  extends BeforeAndAfterAll {
  this: Suite =>

  private val maxDuration = 30.seconds
  private val verifySystemShutdown = true

  implicit val system: ActorSystem

  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(actorSystem = system, duration = maxDuration, verifySystemShutdown = verifySystemShutdown)
  }
}