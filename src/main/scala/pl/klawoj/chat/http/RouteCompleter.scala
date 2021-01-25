package pl.klawoj.chat.http


import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RouteDirectives

trait RouteCompleter {
  def complete(param: (StatusCode, String)): StandardRoute =
    RouteDirectives complete param

  def complete(param: StatusCode): StandardRoute =
    RouteDirectives complete param

  def unauthorized: StandardRoute =
    complete(Unauthorized → "Unauthorized")

  def forbidden: StandardRoute =
    complete(Forbidden → "The supplied authentication is not authorized to access this resource")

  def notFound(clientMessage: Option[String] = None): StandardRoute = {
    complete(NotFound → enrichCompletionMessage("Not found", clientMessage))
  }

  def badRequest(clientMessage: Option[String] = None): StandardRoute = {
    complete(BadRequest → enrichCompletionMessage("Bad Request", clientMessage))
  }

  private def enrichCompletionMessage(baseMsg: String, clientMessage: Option[String]): String = {
    clientMessage.map(extraMsg => s"$baseMsg: $extraMsg").getOrElse(baseMsg)
  }

}

object RouteCompleter extends RouteCompleter