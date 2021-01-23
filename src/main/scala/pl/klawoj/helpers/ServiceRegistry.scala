package pl.klawoj.helpers


import akka.actor.{ActorContext, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import pl.klawoj.helpers.ClassOfT.{classNameOfT, classOfT}

import scala.collection.mutable
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

trait ServiceRegistrySugar {
  def sendTo[T: TypeTag](message: Any): Unit =
    ServiceRegistry sendTo[T] message

  def askHim[T: TypeTag : ClassTag, R](message: Any)(implicit timeout: Timeout): Future[R] =
    ServiceRegistry askHim[T, R] message

  def askHimByName[T: TypeTag : ClassTag, R](messageFactory: => Any)(implicit timeout: Timeout): Future[R] =
    ServiceRegistry askHimByName[T, R] messageFactory

  def forwardTo[T: TypeTag : ClassTag](message: Any)(implicit context: ActorContext): Unit =
    ServiceRegistry forwardTo[T] message

  def forwardTo[T: TypeTag : ClassTag](message: Any, origSender: ActorRef): Unit =
    ServiceRegistry forwardTo[T](message, origSender)
}

trait ServiceRegistry {
  def register[T](actorRef: ActorRef)(implicit ev: TypeTag[T]): Unit

  def find[T](implicit ev: TypeTag[T]): Option[ActorRef]

  def sendTo[T: TypeTag](message: Any): Unit

  def askHim[T: TypeTag : ClassTag, R](message: Any)(implicit timeout: Timeout): Future[R]

  def askHimByName[T: TypeTag : ClassTag, R](messageFactory: => Any)(implicit timeout: Timeout): Future[R]

  def forwardTo[T: TypeTag : ClassTag](message: Any)(implicit context: ActorContext): Unit

  /**
   * Use this function if you do not want to close over ActorContext.
   */
  def forwardTo[T: TypeTag : ClassTag](message: Any, origSender: ActorRef): Unit
}

object ServiceRegistry
  extends ServiceRegistry {

  private val registry: mutable.Map[String, ActorRef] = mutable.Map.empty

  def register[T](actorRef: ActorRef)(implicit ev: TypeTag[T]): Unit = {
    val className = classNameOfT[T]
    registry(className) = actorRef
  }

  def find[T](implicit ev: TypeTag[T]): Option[ActorRef] = {
    val className = classNameOfT[T]
    registry.get(className)
  }

  def sendTo[T: TypeTag](message: Any): Unit =
    Try(message).map { message =>
      ServiceRegistry
        .find[T]
        .foreach(_ ! message)
    }

  def askHimByName[T: TypeTag : ClassTag, R](messageFactory: => Any)(implicit timeout: Timeout): Future[R] =
    byNameMapping(messageFactory) {
      askHim[T, R]
    }

  def askHim[T: TypeTag : ClassTag, R](message: Any)(implicit timeout: Timeout): Future[R] = {
    ServiceRegistry
      .find[T]
      .map(_ ask message)
      .map(_.asInstanceOf[Future[R]])
      .getOrElse(Future.failed(new RuntimeException(s"Service implementing ${classOfT[T].getName} not found")))
  }

  def forwardTo[T: TypeTag : ClassTag](message: Any)(implicit context: ActorContext): Unit =
    ServiceRegistry
      .find[T]
      .foreach(_ forward message)

  def forwardTo[T: TypeTag : ClassTag](message: Any, origSender: ActorRef): Unit =
    ServiceRegistry
      .find[T]
      .foreach(_ tell(message, origSender))

  private def byNameMapping[T](message: => Any)(inner: Any => Future[T]): Future[T] = {
    Try(message) match {
      case Success(v) => inner(v)
      case Failure(ex) => Future.failed(ex)
    }
  }
}