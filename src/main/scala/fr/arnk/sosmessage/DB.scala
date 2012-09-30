package fr.arnk.sosmessage

import com.mongodb.casbah.{ MongoURI, MongoCollection, MongoConnection }
import org.streum.configrity.Configuration

object DB {

  val SosMessageMongoUriParam = "SOS_MESSAGE_MONGO_URI"

  val DefaultMongoUri = "mongodb://localhost/sosmessage"

  lazy val db = {
    val uri = MongoURI(SosMessageConfig.get[String](SosMessageMongoUriParam).getOrElse(DefaultMongoUri))
    val mongo = MongoConnection(uri)
    val db = mongo(uri.database.getOrElse("sosmessage"))
    uri.username.map(name =>
      db.authenticate(name, uri.password.getOrElse(Array("")).foldLeft("")(_ + _.toString))
    )
    db
  }

  def collection[T](name: String)(f: MongoCollection => T): T = f(db(name))

  def drop(name: String) {
    db(name).drop()
  }

}
