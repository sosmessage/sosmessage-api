package fr.arnk.sosmessage

import util.Random
import com.mongodb.DBObject
import java.util.Date
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports._

object SosMessageCollections {
  val MessagesCollectionName = "messages"
  val CategoriesCollectionName = "categories"
  val CommentsCollectionName = "comments"
  val AnnouncementsCollectionName = "announcements"
}

case class Category(dbObject: DBObject)

case class Message(dbObject: DBObject)

case class Comment(dbObject: DBObject)

case class Announcement(dbObject: DBObject)

object SosMessage {

  import SosMessageCollections._

  val DefaultSosMessageAppName = "smdc_fr"

  val random = new Random()

  private val emailSender = EmailSender.get

  // Categories
  def categoryExists(categoryId: String): Boolean = {
    DB.collection(CategoriesCollectionName) {
      c =>
        c.findOne(MongoDBObject("_id" -> new ObjectId(categoryId))) match {
          case Some(o) => true
          case None => false
        }
    }
  }

  def publishedCategories(appName: Option[String]): Seq[Category] = {
    val applicationName = appName match {
      case Some(name) => name
      case None => DefaultSosMessageAppName
    }

    val categoryOrder = MongoDBObject("apps." + applicationName + ".order" -> -1)
    val q = MongoDBObject("apps." + applicationName + ".published" -> true)
    DB.collection(CategoriesCollectionName) {
      c =>
        c.find(q).sort(categoryOrder).toSeq.map(category => Category(category))
    }
  }

  // Messages
  def messageExists(messageId: String): Boolean = {
    DB.collection(MessagesCollectionName) {
      c =>
        c.findOne(MongoDBObject("_id" -> new ObjectId(messageId))) match {
          case Some(o) => true
          case None => false
        }
    }
  }

  def randomMessage(categoryId: String, uid: Option[String]): Option[Message] = {
    DB.collection(MessagesCollectionName) {
      c =>
        val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
        val count = c.find(q, MongoDBObject("_id" -> 1)).count
        val skip = random.nextInt(if (count <= 0) 1 else count)

        val messages = c.find(q).limit(-1).skip(skip)
        if (!messages.isEmpty) {
          val message = computeRatingInformation(messages.next(), uid)
          val json = Message(message)
          Some(json)
        } else {
          None
        }
    }
  }

  def messages(categoryId: String, uid: Option[String]): Seq[Message] = {
    DB.collection(MessagesCollectionName) {
      c =>
        val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
        val order = MongoDBObject("createdAt" -> -1)
        c.find(q).sort(order).toSeq.map(message =>
          Message(computeRatingInformation(message, uid)))
    }
  }

  def bestMessages(categoryId: String, uid: Option[String], limit: Option[Int]): Seq[Message] = {
    DB.collection(MessagesCollectionName) {
      c =>
        val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
        val messages = c.find(q).limit(limit.getOrElse(10)).toSeq.map(message =>
          Message(computeRatingInformation(message, uid)))
        messages.sortBy(m => m.dbObject.get("rating").asInstanceOf[Double]).reverse
    }
  }

  def worstMessages(categoryId: String, uid: Option[String], limit: Option[Int]): Seq[Message] = {
    DB.collection(MessagesCollectionName) {
      c =>
        val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
        val messages = c.find(q).limit(limit.getOrElse(10)).toSeq.map(message =>
          Message(computeRatingInformation(message, uid)))
        messages.sortBy(m => m.dbObject.get("rating").asInstanceOf[Double]).reverse
    }
  }

  def addMessage(categoryId: String, text: String, contributorName: Option[String]) {
    DB.collection(CategoriesCollectionName) {
      c =>
        c.findOne(MongoDBObject("_id" -> new ObjectId(categoryId))) map {
          category =>
            val builder = MongoDBObject.newBuilder
            builder += "categoryId" -> category.get("_id")
            builder += "category" -> category.get("name")
            builder += "text" -> text
            contributorName match {
              case Some(param) =>
                builder += "contributorName" -> param
              case None =>
                builder += "contributorName" -> ""
            }
            builder += "state" -> "waiting"
            builder += "createdAt" -> new Date()
            builder += "modifiedAt" -> new Date()
            builder += "random" -> scala.math.random
            val result = builder.result

            DB.collection(MessagesCollectionName) {
              c =>
                c += result
                emailSender ! SendEmail(result)
            }
        }
    }
  }

  def computeRatingInformation(message: MongoDBObject, uid: Option[String]): MongoDBObject = {
    message.get("ratings") match {
      case None => {
        val builder = MongoDBObject.newBuilder
        builder += ("votePlus" -> 0)
        builder += ("voteMinus" -> 0)
        builder += ("userVote" -> 0)
        builder += ("ratingCount" -> 0)
        builder += ("rating" -> 0.0)
        message.putAll(builder.result())
      }
      case Some(r) => {
        var count: Long = 0
        var total: Double = 0.0
        var votePlus = 0
        var voteMinus = 0
        var userVote = 0

        val ratings = new MongoDBObject(r.asInstanceOf[DBObject])
        for ((k, v) <- ratings) {
          val value = v.asInstanceOf[Int]
          val vote = if (value == 1.0) -1 else 1
          if (uid.isDefined && k.equals(uid.get)) userVote = vote

          if (vote == 1) votePlus += 1 else voteMinus += 1
          count += 1
          total += value
        }

        val builder = MongoDBObject.newBuilder
        builder += ("votePlus" -> votePlus)
        builder += ("voteMinus" -> voteMinus)
        builder += ("userVote" -> userVote)

        val avg = if (total == 0 || count == 0) 0.0 else total / count
        builder += ("ratingCount" -> count)
        builder += ("rating" -> avg)
        message.putAll(builder.result())

        message.removeField("ratings")
      }
    }
    message
  }

  // Rating
  def rateMessage(messageId: String, uid: String, rating: Int): Message = {
    val key = "ratings." + uid.replaceAll("\\.", "-")
    DB.collection(MessagesCollectionName) {
      c =>
        val q = MongoDBObject("_id" -> new ObjectId(messageId))
        c.update(q, $set(key -> rating), false, false)
        Message(computeRatingInformation(c.findOne(q).get, Some(uid)))
    }
  }

  // Comments
  def comments(messageId: String, offset: Option[Int] = Some(0), limit: Option[Int] = Some(10)): Seq[Comment] = {
    val q = MongoDBObject("messageId" -> new ObjectId(messageId))
    val order = MongoDBObject("createdAt" -> 1)
    DB.collection(CommentsCollectionName) {
      c =>
        c.find(q).sort(order).skip(offset.getOrElse(0)).limit(limit.getOrElse(10))
          .toSeq.map(comment => Comment(comment))
    }
  }

  def addComment(messageId: String, uid: String, text: String, author: Option[String] = None): Comment = {
    val oid = new ObjectId(messageId)
    val builder = MongoDBObject.newBuilder
    builder += "messageId" -> oid
    builder += "text" -> text
    author.map({
      a =>
        builder += "author" -> a
    })
    builder += "createdAt" -> new Date()
    builder += "uid" -> uid
    val result = builder.result
    DB.collection(CommentsCollectionName) {
      c =>
        c += result
    }
    DB.collection(MessagesCollectionName) {
      c =>
        c.update(MongoDBObject("_id" -> oid), $inc("commentsCount" -> 1), false, false)
    }
    Comment(result)
  }

  // Announcements
  def publishedAnnouncements(appName: Option[String]): Seq[Announcement] = {
    val applicationName = appName match {
      case Some(name) => name
      case None => DefaultSosMessageAppName
    }

    val q = MongoDBObject("apps." + applicationName + ".published" -> true)
    DB.collection(AnnouncementsCollectionName) {
      c =>
        c.find(q).toSeq.map(announcement => Announcement(announcement))
    }
  }

}
