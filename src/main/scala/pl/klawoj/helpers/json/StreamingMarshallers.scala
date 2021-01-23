package pl.klawoj.helpers.json

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}

object StreamingMarshallers {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport.json()
}
