package pl.klawoj.chat.http

import akka.actor.ActorContext
import akka.http.scaladsl.model.StatusCodes.Created
import akka.http.scaladsl.server.Directives.{complete, get, path, post, _}
import akka.http.scaladsl.server.Route
import pl.klawoj.chat.http.ChatProtocolMarshaller._
import pl.klawoj.helpers.HttpConfig
import pl.klawoj.helpers.json.StreamingMarshallers._

import scala.concurrent.ExecutionContext

class ChatRoute(implicit context: ActorContext, httpConfig: HttpConfig) extends ChatRouteFacade with ExceptionHandling {

  override implicit val dispatcher: ExecutionContext = context.dispatcher

  val route: Route = {


    handleExceptions(exceptionHandler()) {

      pathPrefix("chat" / "user" / Segment) { myId =>
        path("all") {
          onSuccess(getAllUserChats(myId)) { chats =>
            get {
              complete(chats)
            }
          }
        } ~ pathPrefix("with" / Segment) { hisId =>
          path("start") {
            put {
              onSuccess(startChat(myId, hisId)) { chat =>
                complete(Created)
              }
            }
          } ~ path("messages") {
            get {
              onSuccess(getAllChatMessages(myId, hisId)) { messages =>
                complete(messages)
              }
            } ~
              post {
                entity(as[ChatMessageContent]) { message =>
                  onSuccess(postMessageInChat(myId, hisId, message)) { _ =>
                    complete(Created)
                  }
                }
              }
          }

        }
      }
    }
  }
}

object ChatRoute {
  def route(implicit context: ActorContext, httpConfig: HttpConfig): Route = new ChatRoute().route


}