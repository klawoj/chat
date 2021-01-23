package pl.klawoj.chat.http

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import pl.klawoj.chat.http.ChatProtocol.{ChatMessage, OngoingChat}
import pl.klawoj.chat.http.ChatProtocolMarshaller._
import pl.klawoj.helpers.ChatRouteTestConfig

class ChatRouteTest extends AnyWordSpecLike with Matchers with ChatRouteTestConfig with ScalatestRouteTest with ChatRouteTestFixture {


  def getAllUserChats(myId: String): HttpRequest = Get(Uri(s"/chat/user/${myId}/all"))

  def startChat(myId: String, hisId: String): HttpRequest = Put(Uri(s"/chat/user/${myId}/with/${hisId}/start"))

  def getAllChatMessages(myId: String, hisId: String): HttpRequest = Get(Uri(s"/chat/user/${myId}/with/${hisId}/messages"))

  def postMessage(myId: String, hisId: String, message: ChatMessage): HttpRequest = {
    val json = ChatProtocolMarshaller.chatMessageMarshaller.write(message)
    Post(Uri(s"/chat/user/${myId}/with/${hisId}/messages"), HttpEntity(`application/json`, json))
  }

  withHttpConfig { implicit httpConfig =>
    withRoute { implicit route =>
      "Get all user chats " should {
        "return all the chats" when {
          "service returns them properly " in {

            havingGetUserChatsServiceResponseMocked { (id, seq) =>
              getAllUserChats(id) ~> route ~> check {
                status mustBe StatusCodes.OK
                responseAs[Seq[OngoingChat]] mustBe seq
              }
            }
          }
        }
      }

      "Start chat " should {
        "return info about the chat created" when {
          "service returns the info properly " in {
            havingStartChatServiceResponseMocked { (participants, _) =>
              startChat(participants.id1, participants.id2) ~> route ~> check {
                status mustBe StatusCodes.Created
              }
            }
          }
        }
      }

      "Get all chat messages " should {
        "return all the messages from the given chat" when {
          "service returns the info properly " in {
            havingGetAllChatMessagesResponseMocked { (participants, seq) =>
              getAllChatMessages(participants.id1, participants.id2) ~> route ~> check {
                status mustBe StatusCodes.OK
                responseAs[Seq[ChatMessage]] mustBe seq
              }
            }
          }
        }
      }

      "Post message" should {

        "return info about the chat created" when {
          "service returns the info properly " in {
            havingPostMessageResponseMocked { msg =>
              postMessage(msg.participants.id1, msg.participants.id2, msg.chatMessage) ~> route ~> check {
                status mustBe StatusCodes.Created
              }
            }
          }
        }
      }


    }

  }
}
