package pl.klawoj.helpers

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object ClassOfT {
  def classNameOfT[T](implicit tag: TypeTag[T]): String =
    tag.tpe.typeSymbol.asClass.name.toString

  def fullClassNameOfT[T](implicit tag: TypeTag[T]): String =
    tag.tpe.typeSymbol.asClass.fullName.toString

  def classOfT[T](implicit tag: ClassTag[T]): Class[T] =
    tag.runtimeClass.asInstanceOf[Class[T]]
}