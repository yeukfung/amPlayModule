package net.amoeba.services.impl

import scala.concurrent.Future
import play.api.Play
import play.api.libs.ws.WS
import com.ning.http.client.Realm.AuthScheme
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.json.JsObject
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import net.amoeba.core.services.utils.MailingService
import net.amoeba.utils.PlayUtils
import net.amoeba.core.services.utils.MailStatus

class MailingServiceViaMailgun extends MailingService with PlayUtils {

    val messageUrl = playconfig.getString("net.amoeba.services.mailingservice.mailgun.messageUrl").get
    val apiKey = playconfig.getString("net.amoeba.services.mailingservice.mailgun.apiKey").get
    val fromEmail = playconfig.getString("net.amoeba.services.mailingservice.mailgun.fromEmail").get

    def sendMail(to: List[String], subject: String, content: String, cc: List[String] = List(), isHtml: Boolean = false): Future[String] = {
        var param: Map[String, Seq[String]] = Map.empty

        param += ("from" -> Seq(fromEmail))
        param += ("to" -> to)
        param += ("cc" -> cc)
        param += ("subject" -> Seq(subject))
        param += ((if (isHtml) "html" else "text") -> Seq(content))

        val result = WS.url(messageUrl).withAuth("api", apiKey, AuthScheme.BASIC).post(param)

        result.map { resp =>
            //println(s"${resp.status} and also exception result: ${resp.body.toString()}")
            (resp.json \ "id") match {
                case v: JsValue => v.toString
            }
        }
    }

    def getMailStatus(id: String): Future[MailStatus.Type] = {
        ???
    }

}