package pl.klawoj.chat.http

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import pl.klawoj.chat.http.ChatProtocolMarshaller._
import pl.klawoj.helpers.ChatRouteTestConfig

class ChatRouteTest extends AnyWordSpecLike with Matchers with ChatRouteTestConfig with ScalatestRouteTest with ChatRouteTestFixture {

  //TODO test error codes, currently only testing 'happy path'

  def getAllUserChats(myId: String): HttpRequest = Get(Uri(s"/chat/user/${myId}/all"))

  def startChat(myId: String, hisId: String): HttpRequest = Put(Uri(s"/chat/user/${myId}/with/${hisId}/start"))

  def getAllChatMessages(myId: String, hisId: String): HttpRequest = Get(Uri(s"/chat/user/${myId}/with/${hisId}/messages"))

  def postMessage(myId: String, hisId: String, message: ChatMessageContent): HttpRequest = {
    val json = ChatProtocolMarshaller.chatMessageContentMarshaller.write(message)
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
              startChat(participants.senderId, participants.receiverId) ~> route ~> check {
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
              getAllChatMessages(participants.senderId, participants.receiverId) ~> route ~> check {
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
              postMessage(msg.participants.senderId, msg.participants.receiverId, msg.messageContent) ~> route ~> check {
                status mustBe StatusCodes.Created
              }
            }
          }
        }
      }


    }

  }
}
