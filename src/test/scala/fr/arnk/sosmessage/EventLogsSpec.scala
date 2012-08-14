package fr.arnk.sosmessage

import fr.arnk.sosmessage.SosMessageCollections._
import com.mongodb.casbah.commons.MongoDBObject

object EventLogsSpec extends SosMessageSpec {

  "The event logger" should {
    doBefore {
      TestDB.initialize
    }

    "store event logs when getting categories" in {
      http(host / "api" / "v2" / "categories"
        <<? Map("appname" -> "smdc_fr", "uid" -> "ios1") as_str)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "getPublishedCategories"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "ios1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
    }

    "store event logs when getting random message" in {
      val firstCategory = DB.collection(CategoriesCollectionName) {
        c =>
          c.findOne(MongoDBObject("name" -> "firstCategory")).get
      }
      val categoryId = firstCategory.get("_id").toString
      http(host / "api" / "v2" / "categories" / categoryId / "message"
        <<? Map("appname" -> "smdc_fr", "uid" -> "ios1") as_str)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "getRandomMessage"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "ios1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== categoryId
    }

    "store event logs when getting messages" in {
      val secondCategory = DB.collection(CategoriesCollectionName) {
        c =>
          c.findOne(MongoDBObject("name" -> "secondCategory")).get
      }
      val categoryId = secondCategory.get("_id").toString
      http(host / "api" / "v2" / "categories" / categoryId / "messages"
        <<? Map("appname" -> "smdc_fr", "uid" -> "ios1") as_str)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "getMessages"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "ios1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== categoryId
    }

    "store event logs when getting best messages" in {
      val firstCategory = DB.collection(CategoriesCollectionName) {
        c =>
          c.findOne(MongoDBObject("name" -> "firstCategory")).get
      }
      val categoryId = firstCategory.get("_id").toString
      http(host / "api" / "v2" / "categories" / categoryId / "best"
        <<? Map("appname" -> "smdc_fr", "uid" -> "ios1") as_str)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "getBestMessages"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "ios1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== categoryId
    }

    "store event logs when getting worst messages" in {
      val thirdCategory = DB.collection(CategoriesCollectionName) {
        c =>
          c.findOne(MongoDBObject("name" -> "thirdCategory")).get
      }
      val categoryId = thirdCategory.get("_id").toString
      http(host / "api" / "v2" / "categories" / categoryId / "worst"
        <<? Map("appname" -> "smdc_fr", "uid" -> "ios1") as_str)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "getWorstMessages"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "ios1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== categoryId
    }

    "store event logs when posting a message" in {
      val firstCategory = DB.collection(CategoriesCollectionName) {
        c =>
          c.findOne(MongoDBObject("name" -> "firstCategory")).get
      }
      val categoryId = firstCategory.get("_id").toString
      http(host / "api" / "v2" / "categories" / categoryId / "message"
        << Map("text" -> "test message", "appname" -> "smdc_fr", "uid" -> "android1") >|)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "postMessage"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "android1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== categoryId
    }

    "store event logs when rating a message" in {
      val message = DB.collection(MessagesCollectionName) {
        c =>
          c.findOne(MongoDBObject("text" -> "Second message in second category")).get
      }
      val messageId = message.get("_id").toString
      http(host / "api" / "v2" / "messages" / messageId / "rate"
        << Map("uid" -> "iphone1", "rating" -> "4", "appname" -> "smdc_fr") >|)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "rateMessage"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "iphone1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== messageId
    }

    "store event logs when voting a message" in {
      val message = DB.collection(MessagesCollectionName) {
        c =>
          c.findOne(MongoDBObject("text" -> "Second message in second category")).get
      }
      val messageId = message.get("_id").toString
      http(host / "api" / "v2" / "messages" / messageId / "vote"
        << Map("uid" -> "iphone1", "vote" -> "1", "appname" -> "smdc_en") >|)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "voteMessage"
      eventLog.get("appName") must_== "smdc_en"
      eventLog.get("uid") must_== "iphone1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== messageId
    }

    "store event logs when getting comments" in {
      val message = DB.collection(MessagesCollectionName) {
        c =>
          c.findOne(MongoDBObject("text" -> "Second message in second category")).get
      }
      val messageId = message.get("_id").toString
      http(host / "api" / "v2" / "messages" / messageId / "comments"
        <<? Map("appname" -> "smdc_fr", "uid" -> "ios1") as_str)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "getComments"
      eventLog.get("appName") must_== "smdc_fr"
      eventLog.get("uid") must_== "ios1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== messageId
    }

    "store event logs when posting a comment" in {
      val message = DB.collection(MessagesCollectionName) {
        c =>
          c.findOne(MongoDBObject("text" -> "Second message in second category")).get
      }
      val messageId = message.get("_id").toString
      http(host / "api" / "v2" / "messages" / messageId / "comments"
        << Map("text" -> "Bender's comment", "author" -> "Bender", "uid" -> "android1", "appname" -> "smdc_en") >|)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "postComment"
      eventLog.get("appName") must_== "smdc_en"
      eventLog.get("uid") must_== "android1"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
      eventLog.get("targetObject") must_== messageId
    }

    "store event logs when getting annoucements" in {
      http(host / "api" / "v2" / "announcements"
        <<? Map("appname" -> "smdt", "uid" -> "iphone2") as_str)

      waitForEventLogs()

      val eventLogs = DB.collection(EventLogger.EventlogsCollectionName) {
        c =>
          c.find().toSeq
      }
      eventLogs.size must_== 1

      val eventLog = eventLogs(0)
      eventLog.get("action") must_== "getAnnouncements"
      eventLog.get("appName") must_== "smdt_fr"
      eventLog.get("uid") must_== "iphone2"
      eventLog.get("apiVersion") must_== 2
      eventLog.get("appOs") must_== "unknown"
      eventLog.get("appVersion") must_== 0
    }
  }

  def waitForEventLogs() {
    Thread.sleep(3000)
  }

}
