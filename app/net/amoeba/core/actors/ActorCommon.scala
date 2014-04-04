package net.amoeba.core.actors

import net.amoeba.core.services.DBModel
import akka.actor.Actor
import akka.actor.ActorLogging
import net.amoeba.core.actors.ActorCommon.withCRUDReqResp
import akka.actor.ActorRef
import net.amoeba.core.actors.ActorCommon.Req
import net.amoeba.core.actors.ActorCommon.Resp
import net.amoeba.core.services.ReactiveMongoDBService
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Success
import scala.util.Failure
import net.amoeba.core.actors.ActorCommon.Failed
import play.api.libs.json.JsObject
import net.amoeba.core.actors.ActorCommon.withMongoCRUDReqResp
import scala.annotation.tailrec
import scala.util.Random

object ActorCommon {

    object ReqIdGenerator {

        val alphaNumeric = Array[Char]('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9')

        private def toBase(str: Array[Char])(originalNum: Long, prefix: String = "", postfixRandomDigit: Int = 5): String = {

            @tailrec def toBaseInternal(acc: Array[Char], reminder: Long, num: Long, count: Int): Array[Char] = {
                if (num == 0 && reminder == 0) {
                    acc
                } else {
                    val rem = num % str.length
                    val quo = num / str.length
                    val ch: Option[Char] = if (reminder == -1 || num != 0) Some(str(rem.toInt)) else None
                    ch match {
                        case Some(c) =>
                            val idx = acc.length - (count + 1)
                            acc(idx) = c
                            toBaseInternal(acc, rem, quo, count + 1)
                        case None => toBaseInternal(acc, rem, quo, count)
                    }
                }
            }

            // gen random number at the last num of digit of the acc
            @tailrec def genRandomNumberOfDigit(acc: Array[Char], num: Int): Array[Char] = {
                if (num <= 0) acc
                else {
                    val idx = acc.length - num
                    acc(idx) = str(Random.nextInt(str.length()))
                    genRandomNumberOfDigit(acc, num - 1)
                }
            }

            val bigArray = new Array[Char](30)
            val initalList: Array[Char] = genRandomNumberOfDigit(bigArray, postfixRandomDigit)
            prefix + new String(toBaseInternal(initalList, -1, originalNum, postfixRandomDigit)).trim()
        }

        def to62(originalNum: Long, prefix: String = "", prefixRandomDigit: Int = 5) = toBase(alphaNumeric)(originalNum, prefix, prefixRandomDigit)

    }

    def genReqId = ReqIdGenerator.to62(System.currentTimeMillis(), "")

    trait Req { def reqId: String }
    trait Resp { def reqId: String }

    trait Failed extends Resp {
        def req: Req
        def errCode: String
        def errMsg: String
        def ex: Option[Throwable]
        def reqId = req.reqId
    }

    trait withCRUDReqResp[T, RAWTYPE] {
        /** req **/
        case class Create(t: T, reqId: String = genReqId) extends Req
        case class Update(t: T, reqId: String = genReqId) extends Req
        case class UpdatePartial(id: Long, partialObj: RAWTYPE, reqId: String = genReqId) extends Req
        case class Get(id: String, reqId: String = genReqId) extends Req
        case class RawQuery(criteria: RAWTYPE, offset: Long, limit: Int, reqId: String = genReqId) extends Req
        case class Delete(id: String, reqId: String = genReqId) extends Req

        /** resp **/
        case class CreateOk(id: String, newObj: T, reqId: String) extends Resp
        case class UpdateOk(newObj: T, reqId: String) extends Resp
        case class UpdatePartialOk(ok: Boolean, reqId: String = genReqId) extends Req
        case class RawQueryOk(results: List[T], reqId: String) extends Resp
        case class DeleteOk(flag: Boolean, reqId: String) extends Resp
        case class GetOk(result: Option[T], reqId: String) extends Resp

    }

    trait withMongoCRUDReqResp[T] extends withCRUDReqResp[T, JsObject]
}

trait BaseReqRespActor extends Actor with ActorLogging {
    var reqs: Map[String, ActorRef] = Map.empty()

    /** abstract class **/
    def doReq(req: Req)

    def doResp(resp: Resp)

    /** receive **/
    def BaseReqRespReceive: Receive = {
        case req: Req =>
            reqs += (req.reqId -> sender)
            doReq(req)

        case resp: Resp =>
            reqs -= (resp.reqId)
            doResp(resp)

        case _ => println("invalid request")
    }

    def receive = BaseReqRespReceive

}

object CRUDReqRespActor {

    val errorCodesPrefix = s"err.${CRUDReqRespActor.getClass().toString().toLowerCase()}"

    object errorCodes {
        val CREATE_FAILED = s"$errorCodesPrefix.createfailed"
        val UPDATE_FAILED = s"$errorCodesPrefix.updatefailed"
        val GET_FAILED = s"$errorCodesPrefix.getfailed"
        val QUERY_FAILED = s"$errorCodesPrefix.queryfailed"
        val DELETE_FAILED = s"$errorCodesPrefix.deletefailed"
    }
}

//abstract class CRUDReqRespActor[T <: DBModel[T]](dbService: ReactiveMongoDBService[T]) extends BaseReqRespActor with withMongoCRUDReqResp[T] {
//
//    import CRUDReqRespActor.errorCodes._
//    def CRUDReqRespReceive: Receive = {
//
//        /**
//         *  Create
//         */
//        case req @ Create(t, reqId) =>
//            val requestor = sender
//            dbService.insert(t) onComplete {
//                case Success(id) =>
//                    requestor ! CreateOk(id, t.copyWithId(id), reqId)
//                case Failure(t: Throwable) =>
//                    requestor ! Failed(req, CREATE_FAILED, t.getMessage(), Some(t), reqId)
//            }
//
//        /**
//         *  Update
//         */
//        case req @ Update(t, reqId) =>
//            val requestor = sender
//            dbService.update(t) onComplete {
//                case Success(flag) =>
//                    requestor ! UpdateOk(t, reqId)
//                case Failure(t: Throwable) =>
//                    requestor ! Failed(req, UPDATE_FAILED, t.getMessage(), Some(t), reqId)
//            }
//
//        /**
//         *  Get
//         */
//        case req @ Get(id, reqId) =>
//            val requestor = sender
//            dbService.get(id) onComplete {
//                case Success(obj) =>
//                    requestor ! GetOk(obj, reqId)
//                case Failure(t: Throwable) =>
//                    requestor ! Failed(req, GET_FAILED, t.getMessage(), Some(t), reqId)
//            }
//
//        /**
//         *  Query
//         */
//        case req @ RawQuery(criteria, offset, limit, reqId) =>
//            val requestor = sender
//            dbService.find(criteria, offset, limit) onComplete {
//                case Success(obj) =>
//                    requestor ! RawQueryOk(obj, reqId)
//                case Failure(t: Throwable) =>
//                    requestor ! Failed(req, QUERY_FAILED, t.getMessage(), Some(t), reqId)
//            }
//
//        /**
//         *  Delete
//         */
//        case req @ Delete(id, reqId) =>
//            val requestor = sender
//            dbService.delete(id) onComplete {
//                case Success(flag) =>
//                    requestor ! DeleteOk(flag, reqId)
//                case Failure(t: Throwable) =>
//                    requestor ! Failed(req, DELETE_FAILED, t.getMessage(), Some(t), reqId)
//            }
//
//    }
//
//    override def receive = CRUDReqRespReceive orElse BaseReqRespReceive
//}