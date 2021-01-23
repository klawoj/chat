package pl.klawoj.helpers.json

trait Marshallable[T <: AnyRef] {
  def read(json: String): T

  def write(o: T): String

  def writeToArray(o: T): Array[Byte]
}

trait Marshaller {
  def read[T <: AnyRef : Manifest](json: String)(implicit marshallable: Marshallable[T]): T =
    marshallable.read(json)

  def write[T <: AnyRef : Manifest](o: T)(implicit marshallable: Marshallable[T]): String =
    marshallable.write(o)

  def writeToArray[T <: AnyRef : Manifest](o: T)(implicit marshallable: Marshallable[T]): Array[Byte] =
    marshallable.writeToArray(o)
}
