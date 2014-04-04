package net.amoeba.automatic.actors.utils

import com.google.inject.Inject
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints
import scala.util.matching.Regex
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import net.amoeba.utils.ut
import net.amoeba.core.services.utils.MailingService
import net.amoeba.core.actors.ActorCommon
import net.amoeba.core.actors.ActorCommon._
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.Actor
import akka.actor.ActorLogging

/**
 * this actor aims to accept mailing messages
 */
object MailingActor {

    val errorCodesPrefix = s"err.${MailingActor.getClass().toString().toLowerCase()}"

    object errorCodes {
        val MAIL_FAILED = s"$errorCodesPrefix.mailfailed"
    }

    case class Mail(to: List[String], subject: String, content: String, isHtml: Boolean, reqId: String = ActorCommon.genReqId) extends Req

    case class MailOk(reqId: String) extends Resp

    case class MailEx(req: Req, errCode: String, errMsg: String, ex: Option[Throwable]) extends Exception with Failed

}

class MailingActor @Inject() (mailService: MailingService) extends Actor with ActorLogging {

    import MailingActor._
    import MailingActor.errorCodes._

    def receive = {
        case req @ Mail(to, subject, content, isHtml, reqId) =>
            log.debug(s"received mailing request: $to $subject")
            val requestor = sender
            var errMsg: List[String] = List.empty

            val finalTo = to.filter(ut.isEmail(_)).size

            if (finalTo == 0) errMsg ::= "error.invalid.to";
            if (ut.isEmail(subject)) errMsg ::= "error.invalid.subject";
            if (ut.isEmail(content)) errMsg ::= "error.invalid.content";

            if (errMsg.size > 0) requestor ! MailEx(req, MAIL_FAILED, errMsg.foldLeft("")((a, i) => a + i), None)
            else {
                mailService.sendMail(to, subject, content, isHtml = isHtml).onComplete {
                    case Success(x) =>
                        requestor ! MailOk(reqId);
                    case Failure(t) =>
                        requestor ! MailEx(req, MAIL_FAILED, t.getMessage, Some(t));
                }
            }

    }

}