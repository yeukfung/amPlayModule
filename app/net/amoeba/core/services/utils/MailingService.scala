package net.amoeba.core.services.utils

import scala.concurrent.Future

object MailStatus extends Enumeration {
    type Type = Value
    val SUBMITTED, SUCCESS, FAILED = Value
}

trait MailingService {

    def sendMail(to: List[String], subject: String, content: String, cc: List[String] = List(), isHtml: Boolean = false): Future[String]

    def getMailStatus(id: String): Future[MailStatus.Type]

}