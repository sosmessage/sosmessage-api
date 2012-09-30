package fr.arnk.sosmessage

import org.streum.configrity.converter.ValueConverter
import org.streum.configrity._

object SosMessageConfig {

  val defaultConfig = Configuration("SOS_MESSAGE_MONGO_URI" -> "mongodb://localhost/sosmessage",
    "PORT" -> 3000)

  val env = Configuration.environment

  private var config: Configuration = defaultConfig

  def apply[T](key: String)(implicit converter: ValueConverter[T]) = {
    get[T](key)
  }

  def get[T](key: String)(implicit converter: ValueConverter[T]) = {
    env.get[T](key) match {
      case None => config.get[T](key)
      case Some(s) => Some(s)
    }
  }

  def set[T](key: String, value: T) = {
    config = config.set(key, value)
  }

}
