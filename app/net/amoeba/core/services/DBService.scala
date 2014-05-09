package net.amoeba.core.services

import scala.concurrent._
import play.api.libs.json.JsObject
import java.util.Date

trait WithCreatedDateTime {
    def created: Option[Long]
    def modified: Option[Long]
}

trait DBModel[T] {
    def id: Option[String]
    def copyWithId(id: String): T
}

trait ReactiveMongoDBService[T <: DBModel[T]] extends CoreReactiveDBService[T, String, JsObject, JsObject]

trait CoreReactiveDBService[T, Id, Query, Update] {

    implicit def defaultQueryHook(q: JsObject): JsObject = q

    def insert(t: T, queryHook: (JsObject) => JsObject = defaultQueryHook)(implicit ctx: ExecutionContext): Future[Id]

    def get(id: Id, queryHook: (JsObject) => JsObject = defaultQueryHook)(implicit ctx: ExecutionContext): Future[Option[T]]

    def delete(id: Id, queryHook: (JsObject) => JsObject = defaultQueryHook)(implicit ctx: ExecutionContext): Future[Boolean]

    def update(t: T, queryHook: (JsObject) => JsObject = defaultQueryHook)(implicit ctx: ExecutionContext): Future[Boolean]

    def updatePartial(id: Id, upd: Update, queryHook: (JsObject) => JsObject = defaultQueryHook)(implicit ctx: ExecutionContext): Future[Boolean]

    def find(sel: Query, limit: Long = 0, skip: Long = 0, queryHook: (JsObject) => JsObject = defaultQueryHook)(implicit ctx: ExecutionContext): Future[List[T]]

    def findOne(sel: Query, queryHook: (JsObject) => JsObject = defaultQueryHook)(implicit ctx: ExecutionContext): Future[Option[T]] = find(sel, 1, 0, queryHook) map (_.headOption)

    /** TODO: to impl once required **/
    /*
    def findStream(sel: Query, skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[TraversableOnce[(T, Id)]]

    def batchInsert(elems: Enumerator[T])(implicit ctx: ExecutionContext): Future[BatchReturn]

    def batchDelete(sel: Query)(implicit ctx: ExecutionContext): Future[BatchReturn]

    def batchUpdate(sel: Query, upd: Update)(implicit ctx: ExecutionContext): Future[BatchReturn]
    */

}
