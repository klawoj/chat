package pl.klawoj.helpers

import akka.util.Timeout

import scala.concurrent.duration.DurationInt

trait ServiceCallTimeout {
  implicit val serviceCallTimeout: Timeout = 10.seconds
}