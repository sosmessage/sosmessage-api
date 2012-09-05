package fr.arnk.sosmessage

import akka.actor.{ Actor, Props, ActorSystem }
import com.mongodb.casbah.Imports._

case class Event(data: Map[String, Any])

object EventLogger {

  val EventlogsCollectionName = "eventlogs"
  private val system = ActorSystem("EventLoggerSystem")
  private val eventLogger = system.actorOf(Props(new EventLogger), name = "eventLogger")

  def logEvent(data: Map[String, Any]) {
    eventLogger ! Event(data)
  }

}

class EventLogger extends Actor {
  import EventLogger._

  def receive = {
    case Event(data) =>
      logEvent(data)
  }

  def logEvent(data: Map[String, Any]) {
    DB.collection(EventlogsCollectionName) {
      c =>
        c += data.asDBObject
    }
  }

}
