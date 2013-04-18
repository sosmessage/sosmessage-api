package fr.arnk.sosmessage

import akka.actor._
import com.mongodb.DBObject
import javax.mail.internet.{ InternetAddress, MimeMessage }
import javax.mail.{ Authenticator, Message => JMessage, PasswordAuthentication, Session }

case class SendEmail(message: DBObject)

object EmailSender {

  private val system = ActorSystem("EmaiSenderSystem")
  private val emailSender = system.actorOf(Props(new EmailSender), name = "emailSender")

  def get = {
    emailSender
  }

  def stop() {
    system.stop(emailSender)
  }

}

class EmailSender extends Actor {

  private val Subject = "[Moderation] New message waiting for approval"

  private val Text = """
    Hi,

    There is a new message waiting your approval!

    Category:
      %s

    Message:
      %s

    Contributed by %s
  """

  def receive = {
    case SendEmail(message) =>
      val mailEnabled = SosMessageConfig[Boolean]("SOS_MESSAGE_MAIL_ENABLED").getOrElse(false)
      if (mailEnabled) {
        sendEmail(message)
      }
  }

  def sendEmail(message: DBObject) {
    val tls = SosMessageConfig[String]("SOS_MESSAGE_MAIL_TLS").getOrElse("false")
    val auth = SosMessageConfig[String]("SOS_MESSAGE_MAIL_AUTH").getOrElse("false")
    val host = SosMessageConfig[String]("SOS_MESSAGE_MAIL_HOST").get
    val port = SosMessageConfig[Int]("SOS_MESSAGE_MAIL_PORT").get
    val user = SosMessageConfig[String]("SOS_MESSAGE_MAIL_USER").get
    val password = SosMessageConfig[String]("SOS_MESSAGE_MAIL_PASSWORD").get

    val props = System.getProperties
    if (auth == "true") {
      props.put("mail.smtp.auth", "true")
      props.put("mail.smtp.user", user)
      props.put("mail.smtp.password", password)
    } else {
      props.put("mail.smtp.auth", "false")
    }
    props.put("mail.smtp.starttls.enable", tls)
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port.toString)
    // props.put("mail.smtp.socketFactory.port", port.toString);
    // props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    // val authenticator = new Authenticator() {
    //   override def getPasswordAuthentication: PasswordAuthentication = {
    //     new PasswordAuthentication(user, password)
    //   }
    // }
    // val session = Session.getDefaultInstance(props, authenticator)
    val session = Session.getDefaultInstance(props)
    val mimeMessage = new MimeMessage(session)

    mimeMessage.setFrom(new InternetAddress(SosMessageConfig[String]("SOS_MESSAGE_MAIL_FROM").get))
    mimeMessage.setRecipients(JMessage.RecipientType.TO, SosMessageConfig[String]("SOS_MESSAGE_MAIL_RECIPIENTS").get)
    mimeMessage.setSubject(Subject)
    val text = Text.format(message.get("category").toString, message.get("text").toString, message.get("contributorName").toString)
    mimeMessage.setText(text)

    val transport = session.getTransport("smtp")
    transport.connect(host, port, user, password)
    transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients)
    transport.close()
  }
}
