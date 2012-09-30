package fr.arnk.sosmessage

import org.specs._
import unfiltered._
import dispatch._

class SosMessageSpec extends Specification with unfiltered.spec.netty.Served {

  // Initialize test config
  SosMessageConfig.set("SOS_MESSAGE_MONGO_URI", "mongodb://localhost/sosmessagedetest")

  def setup = {
    _.handler(netty.cycle.Planify {
      SosMessageApi.publishedCategories orElse
        SosMessageApi.messages orElse SosMessageApi.randomMessage orElse
        SosMessageApi.bestMessages orElse SosMessageApi.worstMessages orElse
        SosMessageApi.postMessage orElse SosMessageApi.rateMessage orElse SosMessageApi.voteMessage orElse
        SosMessageApi.commentsForMessage orElse SosMessageApi.postComment orElse
        SosMessageApi.publishedAnnouncements orElse
        SosMessageApiV1.publishedCategories orElse
        SosMessageApiV1.messages orElse SosMessageApiV1.randomMessage orElse
        SosMessageApiV1.bestMessages orElse SosMessageApiV1.worstMessages orElse
        SosMessageApiV1.postMessage orElse SosMessageApiV1.rateMessage orElse SosMessageApiV1.voteMessage orElse
        SosMessageApiV1.commentsForMessage orElse SosMessageApiV1.postComment orElse
        SosMessageApiV1.publishedAnnouncements
    })
  }

  override def http[T](handler: Handler[T]): T = {
    val h = new Http
    try {
      (h.when { code => (200 to 500) contains code })(handler)
    } finally {
      h.shutdown()
    }
  }
}
