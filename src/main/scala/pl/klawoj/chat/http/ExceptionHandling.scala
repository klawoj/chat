package pl.klawoj.chat.http

import akka.http.scaladsl.server.ExceptionHandler

trait ExceptionHandling {
  def exceptionHandler(routeCompleter: RouteCompleter = RouteCompleter): ExceptionHandler = ExceptionHandler {

    //TODO improve this basic exception handling (added quickly not to have 500 on every error) and add application specific exceptions

    case e: IllegalArgumentException =>
      routeCompleter.badRequest(Option(e.getMessage))

    case e: NoSuchElementException =>
      routeCompleter.notFound(Option(e.getMessage))

  }
}