package fr.arnk.sosmessage

import unfiltered.Cycle
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import unfiltered.request._
import unfiltered.response._
import StandardConverters._
import unfiltered._
import java.util.Date
import org.slf4j.LoggerFactory

object SosMessageApi {

  val ApiVersion = 2

  val DEFAULT_LANG_SUFFIX = "_fr"

  val logger = LoggerFactory.getLogger("SosMessageApi")

  // Categories
  def publishedCategories: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/v2/categories")) =>
      withErrorHandling {
        val Params(form) = req
        val appName = form.get("appname") match {
          case Some(params) => Some(computeFullAppName(params(0)))
          case None => None
        }

        val categories = SosMessage.publishedCategories(appName)
        EventLogger.logEvent(computeEventData(req, "getPublishedCategories", None))
        val json = ("meta", ("code", 200)) ~
          ("response", (("count", categories.size) ~ ("items", toJSON(categories))))
        Ok ~> Json(json)
      }
  }

  // Messages
  def randomMessage: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("v2" :: "categories" :: id :: "message" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        val uid = form.get("uid") match {
          case Some(param) => Some(param(0))
          case None => None
        }

        if (SosMessage.categoryExists(id)) {
          SosMessage.randomMessage(id, uid) match {
            case None => NoContent
            case Some(message) => {
              EventLogger.logEvent(computeEventData(req, "getRandomMessage", Some(id)))
              val json = ("meta", ("code", 200)) ~
                ("response", toJSON(message))
              Ok ~> Json(json)
            }
          }
        } else {
          BadRequest ~> buildBadRequestResponse("UnknownCategory",
            "The category does not exist.")
        }
      }
  }

  def messages: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("v2" :: "categories" :: id :: "messages" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        val uid = form.get("uid") match {
          case Some(param) => Some(param(0))
          case None => None
        }

        if (SosMessage.categoryExists(id)) {
          val messages = SosMessage.messages(id, uid)
          EventLogger.logEvent(computeEventData(req, "getMessages", Some(id)))
          val json = ("meta", ("code", 200)) ~
            ("response", ("count", messages.size) ~ ("items", toJSON(messages)))
          Ok ~> Json(json)
        } else {
          BadRequest ~> buildBadRequestResponse("UnknownCategory",
            "The category does not exist.")
        }
      }
  }

  def bestMessages: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("v2" :: "categories" :: id :: "best" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        val uid = form.get("uid") match {
          case Some(param) => Some(param(0))
          case None => None
        }
        val limit = form.get("limit") match {
          case Some(param) => Some(param(0).toInt)
          case None => None
        }

        if (SosMessage.categoryExists(id)) {
          val messages = SosMessage.bestMessages(id, uid, limit)
          EventLogger.logEvent(computeEventData(req, "getBestMessages", Some(id)))
          val json = ("meta", ("code", 200)) ~
            ("response", ("count", messages.size) ~ ("items", toJSON(messages)))
          Ok ~> Json(json)
        } else {
          BadRequest ~> buildBadRequestResponse("UnknownCategory",
            "The category does not exist.")
        }
      }
  }

  def worstMessages: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("v2" :: "categories" :: id :: "worst" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        val uid = form.get("uid") match {
          case Some(param) => Some(param(0))
          case None => None
        }
        val limit = form.get("limit") match {
          case Some(param) => Some(param(0).toInt)
          case None => None
        }

        if (SosMessage.categoryExists(id)) {
          val messages = SosMessage.worstMessages(id, uid, limit)
          EventLogger.logEvent(computeEventData(req, "getWorstMessages", Some(id)))
          val json = ("meta", ("code", 200)) ~
            ("response", ("count", messages.size) ~ ("items", toJSON(messages)))
          Ok ~> Json(json)
        } else {
          BadRequest ~> buildBadRequestResponse("UnknownCategory",
            "The category does not exist.")
        }
      }
  }

  def postMessage: Cycle.Intent[Any, Any] = {
    case req @ POST(Path(Seg("v2" :: "categories" :: categoryId :: "message" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        if (!form.contains("text")) {
          BadRequest ~> buildBadRequestResponse("MissingParameter",
            "The 'text' parameter is required.")
        } else if (!SosMessage.categoryExists(categoryId)) {
          BadRequest ~> buildBadRequestResponse("UnknownCategory",
            "The category does not exist.")
        } else {
          val text = form.get("text").get(0)
          val contributorName = form.get("contributorName") match {
            case Some(param) => Some(param(0))
            case None => None
          }
          SosMessage.addMessage(categoryId, text, contributorName)
          EventLogger.logEvent(computeEventData(req, "postMessage", Some(categoryId)))
          val json = ("meta", ("code", 200)) ~
            ("response", JObject(List()))
          Ok ~> Json(json)
        }
      }
  }

  def rateMessage: Cycle.Intent[Any, Any] = {
    case req @ POST(Path(Seg("v2" :: "messages" :: messageId :: "rate" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        if (!SosMessage.messageExists(messageId)) {
          BadRequest ~> buildBadRequestResponse("UnknownMessage",
            "The message does not exist.")
        } else if (!form.contains("uid")) {
          BadRequest ~> buildBadRequestResponse("MissingParameter",
            "The 'uid' parameter is required.")
        } else if (!form.contains("rating")) {
          BadRequest ~> buildBadRequestResponse("MissingParameter",
            "The 'rating' parameter is required.")
        } else {
          val uid = form("uid")(0)
          val rating = if (form("rating")(0).toInt > 5) 5 else form("rating")(0).toInt
          val message = SosMessage.rateMessage(messageId, uid, rating)
          EventLogger.logEvent(computeEventData(req, "rateMessage", Some(messageId)))
          val json = ("meta", ("code", 200)) ~
            ("response", toJSON(message))
          Ok ~> Json(json)
        }
      }
  }

  def voteMessage: Cycle.Intent[Any, Any] = {
    case req @ POST(Path(Seg("v2" :: "messages" :: messageId :: "vote" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        if (!SosMessage.messageExists(messageId)) {
          BadRequest ~> buildBadRequestResponse("UnknownMessage",
            "The message does not exist.")
        } else if (!form.contains("uid")) {
          BadRequest ~> buildBadRequestResponse("MissingParameter",
            "The 'uid' parameter is required.")
        } else if (!form.contains("vote")) {
          BadRequest ~> buildBadRequestResponse("MissingParameter",
            "The 'vote' parameter is required.")
        } else {
          val uid = form("uid")(0)
          val vote = form("vote")(0).toInt
          if (vote != 1 && vote != -1) {
            BadRequest ~> buildBadRequestResponse("WrongParameter",
              "The 'vote' parameter must be -1 or 1.")
          } else {
            val rating = if (vote == 1) 5 else 1
            val message = SosMessage.rateMessage(messageId, uid, rating)
            EventLogger.logEvent(computeEventData(req, "voteMessage", Some(messageId)))
            val json = ("meta", ("code", 200)) ~
              ("response", toJSON(message))
            Ok ~> Json(json)
          }
        }
      }
  }

  // Comments
  def commentsForMessage: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("v2" :: "messages" :: messageId :: "comments" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        val offset = form.get("offset") match {
          case Some(param) => Some(param(0).toInt)
          case None => None
        }
        val limit = form.get("limit") match {
          case Some(param) => Some(param(0).toInt)
          case None => None
        }

        val comments = SosMessage.comments(messageId, offset, limit)
        EventLogger.logEvent(computeEventData(req, "getComments", Some(messageId)))
        val json = ("meta", ("code", 200)) ~
          ("response", ("count", comments.size) ~ ("items", toJSON(comments)))
        Ok ~> Json(json)
      }
  }

  def postComment: Cycle.Intent[Any, Any] = {
    case req @ POST(Path(Seg("v2" :: "messages" :: messageId :: "comments" :: Nil))) =>
      withErrorHandling {
        val Params(form) = req
        if (!form.contains("uid")) {
          BadRequest ~> buildBadRequestResponse("MissingParameter",
            "The 'uid' parameter is required.")
        } else if (!form.contains("text")) {
          BadRequest ~> buildBadRequestResponse("MissingParameter",
            "The 'text' parameter is required.")
        } else if (!SosMessage.messageExists(messageId)) {
          BadRequest ~> buildBadRequestResponse("UnknownMessage",
            "The message does not exist.")
        } else {
          val uid = form.get("uid").get(0)
          val text = form.get("text").get(0)
          val author = form.get("author") match {
            case Some(param) => Some(param(0))
            case None => None
          }
          SosMessage.addComment(messageId, uid, text, author)
          EventLogger.logEvent(computeEventData(req, "postComment", Some(messageId)))
          val json = ("meta", ("code", 200)) ~
            ("response", JObject(List()))
          Ok ~> Json(json)
        }
      }
  }

  // Announcements
  def publishedAnnouncements: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/v2/announcements")) =>
      withErrorHandling {
        val Params(form) = req
        val appName = form.get("appname") match {
          case Some(params) => Some(computeFullAppName(params(0)))
          case None => None
        }

        val announcements = SosMessage.publishedAnnouncements(appName)
        EventLogger.logEvent(computeEventData(req, "getAnnouncements", None))
        val json = ("meta", ("code", 200)) ~
          ("response", ("count", announcements.size) ~ ("items", toJSON(announcements)))
        Ok ~> Json(json)
      }
  }

  private[this] def withErrorHandling(f: => ResponseFunction[Any]): ResponseFunction[Any] = {
    try {
      f ~> ResponseHeader("Access-Control-Allow-Origin", "*" :: Nil)
    } catch {
      case e: Exception => {
        logger.error(e.getMessage, e)
        val json = ("meta", ("code", 500) ~ ("errorType", "ServerError") ~
          ("errorDetails", e.getMessage)) ~ ("response", JObject(List()))
        InternalServerError ~> Json(json) ~> ResponseHeader("Access-Control-Allow-Origin", "*" :: Nil)
      }
    }
  }

  private[this] def buildBadRequestResponse(errorType: String, errorDetails: String) = {
    val json = ("meta", ("code", 400) ~ ("errorType", errorType) ~
      ("errorDetails", errorDetails)) ~
      ("response", JObject(List()))
    Json(json)
  }

  private[this] def computeEventData(req: HttpRequest[Any], action: String,
    targetObject: Option[String]): Map[String, Any] = {
    val userAgent = UserAgent.apply(req)
    val (appVersion, os) = userAgent match {
      case Some(ua) => {
        val AppVersion = """sosmessage/(.+?) .*""".r
        try {
          val AppVersion(version) = ua
          (version, "ios")
        } catch {
          case e: MatchError => (0, "unknown")
        }
      }
      case None => (0, "unknown")
    }

    val Params(form) = req
    val uid = form.get("uid") match {
      case Some(param) => param(0)
      case None => ""
    }
    val appName = form.get("appname") match {
      case Some(param) => {
        computeFullAppName(param(0))
      }
      case None => "smdc_fr" // default
    }

    Map("apiVersion" -> ApiVersion, "appVersion" -> appVersion, "appOs" -> os,
      "appName" -> appName, "uid" -> uid, "action" -> action,
      "targetObject" -> targetObject.getOrElse(""), "createdAt" -> new Date())
  }

  private[this] def computeFullAppName(appName: String) = {
    if (appName.contains("_")) {
      appName
    } else {
      appName + DEFAULT_LANG_SUFFIX
    }
  }

}
