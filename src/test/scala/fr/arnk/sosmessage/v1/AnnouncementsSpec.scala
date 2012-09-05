package fr.arnk.sosmessage.v1

import fr.arnk.sosmessage._

import net.liftweb.json._

object AnnouncementsSpec extends SosMessageSpec {

  "The announcements API v1" should {
    doBefore {
      TestDB.initialize
    }

    "retrieve only published announcements for given app" in {
      val resp = http(host / "api" / "v1" / "announcements" <<? Map("appname" -> "smdt_fr") as_str)
      val json = parse(resp)
      json \ "count" must_== JInt(1)

      val JArray(items) = json \ "items"
      items.size must_== 1

      val firstItem = items(0)
      firstItem \ "title" must_== JString("Fourth announcement")
      firstItem \ "text" must_== JString("Text of fourth announcement")
      firstItem \ "url" must_== JString("http://fourth/announcement")
    }
  }

}
