package fr.arnk.sosmessage

import org.specs._
import unfiltered._
import org.streum.configrity.Configuration
import com.mongodb.casbah.Imports._
import net.liftweb.json._
import java.util.Date
import com.mongodb.{ BasicDBObject, DBObject }

object CategoriesSpec extends SosMessageSpec {

  import SosMessageCollections._

  "The categories API v2" should {
    doBefore {
      TestDB.initialize
    }

    "retrieve ordered published categories" in {
      val resp = http(host / "v2" / "categories" as_str)
      val json = parse(resp)

      json \ "meta" \ "code" must_== JInt(200)
      val response = json \ "response"
      response \ "count" must_== JInt(3)

      val JArray(items) = response \ "items"
      items.size must_== 3

      val firstItem = items(0)
      firstItem \ "name" must_== JString("firstCategory")
      firstItem \ "color" must_== JString("#000")
      firstItem \ "free" must_== JBool(true)

      val secondItem = items(1)
      secondItem \ "name" must_== JString("secondCategory")
      secondItem \ "color" must_== JString("#fff")
      secondItem \ "free" must_== JBool(true)

      val thirdItem = items(2)
      thirdItem \ "name" must_== JString("fourthCategory")
      thirdItem \ "color" must_== JString("#00f")
      thirdItem \ "free" must_== JBool(false)
    }

    "retrieve ordered published categories for the smdt appname" in {
      val resp = http(host / "v2" / "categories" <<? Map("appname" -> "smdt_fr") as_str)
      val json = parse(resp)

      json \ "meta" \ "code" must_== JInt(200)
      val response = json \ "response"
      response \ "count" must_== JInt(2)

      val JArray(items) = response \ "items"
      items.size must_== 2

      val firstItem = items(0)
      firstItem \ "name" must_== JString("fifthCategory")
      firstItem \ "color" must_== JString("#0ff")

      val secondItem = items(1)
      secondItem \ "name" must_== JString("fourthCategory")
      secondItem \ "color" must_== JString("#00f")
    }

    "retrieve no category for non existing app" in {
      val resp = http(host / "v2" / "categories" <<? Map("appname" -> "nonExistingApp") as_str)
      val json = parse(resp)

      json \ "meta" \ "code" must_== JInt(200)
      val response = json \ "response"
      response \ "count" must_== JInt(0)

      val JArray(items) = response \ "items"
      items.size must_== 0
    }
  }

}
