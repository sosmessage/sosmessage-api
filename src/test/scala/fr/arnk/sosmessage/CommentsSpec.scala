package fr.arnk.sosmessage

import org.specs._
import unfiltered._
import org.streum.configrity.Configuration
import com.mongodb.casbah.Imports._
import net.liftweb.json._
import java.util.Date
import com.mongodb.{ BasicDBObject, DBObject }

object CommentsSpec extends SosMessageSpec {

  import SosMessageCollections._

  "The comments API v2" should {
    doBefore {
      TestDB.initialize
    }

    "create comments for the given message" in {
      DB.collection(MessagesCollectionName) {
        c =>
          val aMessage = c.findOne(MongoDBObject("text" -> "First message in first category")).get
          http(host / "v2" / "messages" / aMessage.get("_id").toString / "comments"
            << Map("text" -> "Bender's comment", "author" -> "Bender", "uid" -> "android1") >|)
          http(host / "v2" / "messages" / aMessage.get("_id").toString / "comments"
            << Map("text" -> "Leela's comment", "author" -> "Leela", "uid" -> "iphone1") >|)

          val updatedMessage = c.findOne(MongoDBObject("text" -> "First message in first category")).get
          updatedMessage.asInstanceOf[BasicDBObject].getLong("commentsCount") must_== 2

          val resp = http(host / "v2" / "messages" / updatedMessage.get("_id").toString / "comments" as_str)
          val json = parse(resp)

          json \ "meta" \ "code" must_== JInt(200)
          val response = json \ "response"
          response \ "count" must_== JInt(2)

          val JArray(items) = response \ "items"
          items.size must_== 2

          val firstItem = items(0)
          firstItem \ "text" must_== JString("Bender's comment")
          firstItem \ "author" must_== JString("Bender")
          firstItem \ "messageId" must_== JString(updatedMessage.get("_id").toString)
          firstItem \ "uid" must_== JString("android1")

          val secondItem = items(1)
          secondItem \ "text" must_== JString("Leela's comment")
          secondItem \ "author" must_== JString("Leela")
          secondItem \ "messageId" must_== JString(updatedMessage.get("_id").toString)
          secondItem \ "uid" must_== JString("iphone1")
      }
    }

    // test error handling
    "return an error when creating comment for non existing message" in {
      DB.collection(CategoriesCollectionName) {
        c =>
          val aCategory = c.findOne(MongoDBObject("name" -> "firstCategory")).get
          val resp = http(host / "v2" / "messages" / aCategory.get("_id").toString / "comments"
            << Map("text" -> "Bender's comment", "author" -> "Bender", "uid" -> "android1") as_str)
          val json = parse(resp)

          json \ "meta" \ "code" must_== JInt(400)
          json \ "meta" \ "errorType" must_== JString("UnknownMessage")
          json \ "meta" \ "errorDetails" must_== JString("The message does not exist.")
          json \ "response" must_== JObject(List())
      }

      val resp = http(host / "v2" / "messages" / "nonExistingId" / "comments"
        << Map("text" -> "Bender's comment", "author" -> "Bender", "uid" -> "android1") as_str)
      val json = parse(resp)

      json \ "meta" \ "code" must_== JInt(500)
      json \ "meta" \ "errorType" must_== JString("ServerError")
      json \ "response" must_== JObject(List())
    }

    "return an error when creating comment without required parameters" in {
      DB.collection(MessagesCollectionName) {
        c =>
          val aMessage = c.findOne(MongoDBObject("text" -> "First message in first category")).get
          var resp = http(host / "v2" / "messages" / aMessage.get("_id").toString / "comments"
            << Map("author" -> "Bender", "uid" -> "android1") as_str)
          var json = parse(resp)

          json \ "meta" \ "code" must_== JInt(400)
          json \ "meta" \ "errorType" must_== JString("MissingParameter")
          json \ "meta" \ "errorDetails" must_== JString("The 'text' parameter is required.")
          json \ "response" must_== JObject(List())

          resp = http(host / "v2" / "messages" / aMessage.get("_id").toString / "comments"
            << Map("text" -> "Bender's comment", "author" -> "Bender") as_str)
          json = parse(resp)

          json \ "meta" \ "code" must_== JInt(400)
          json \ "meta" \ "errorType" must_== JString("MissingParameter")
          json \ "meta" \ "errorDetails" must_== JString("The 'uid' parameter is required.")
          json \ "response" must_== JObject(List())
      }
    }
  }

}
