package pl.klawoj.helpers

import akka.actor.{ActorRef, ActorRefFactory, Props}

import scala.reflect.runtime.universe._

trait ActorFactory[T] {
  val actorName: String
  val dependencies: Array[Any] = Array.empty

  def actor(implicit actorFactory: ActorRefFactory, tag: TypeTag[T]): ActorRef = {
    actor(dependencies: _*)
  }

  def actor(dependencies: Any*)(implicit actorFactory: ActorRefFactory, tag: TypeTag[T]): ActorRef = {
    createActor(props(dependencies: _*))
  }

  private def createActor(props: Props)(implicit actorFactory: ActorRefFactory) = {
    val actorRef = actorFactory.actorOf(props, actorName)
    postCreate(actorRef)
    actorRef
  }

  private def props(dependencies: Any*)(implicit tag: TypeTag[T]): Props = {
    baseProps(dependencies: _*)
  }

  def baseProps(dependencies: Any*)(implicit tag: TypeTag[T]): Props = {
    Props(tag.mirror.runtimeClass(tag.tpe.typeSymbol.asClass), dependencies: _*)
  }

  def postCreate(actorRef: ActorRef): Unit = {
  }
}