package net.amoeba.core.services

import scala.concurrent._
import play.api.libs.json.JsObject
import java.util.Date

trait WithCreatedDateTime {
    def created: Date
    def modified: Date
}

trait DBModel[T] {
    def id: Option[String]
    def copyWithId(id: String): T
}

trait ReactiveMongoDBService[T <: DBModel[T]] extends CoreReactiveDBService[T, String, JsObject, JsObject]

trait CoreReactiveDBService[T <: DBModel[T], Id, Query, Update] {

    def insert(t: T)(implicit ctx: ExecutionContext): Future[Id]

    def get(id: Id)(implicit ctx: ExecutionContext): Future[Option[T]]

    def delete(id: Id)(implicit ctx: ExecutionContext): Future[Boolean]

    def update(t: T)(implicit ctx: ExecutionContext): Future[Boolean]

    def updatePartial(id: Id, upd: Update)(implicit ctx: ExecutionContext): Future[Boolean]

    def find(sel: Query, limit: Long = 0, skip: Long = 0)(implicit ctx: ExecutionContext): Future[List[T]]

    def findOne(sel: Query)(implicit ctx: ExecutionContext): Future[Option[T]] = find(sel, 1) map (_.headOption)

    /** TODO: to impl once required **/
    /*
    def findStream(sel: Query, skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[TraversableOnce[(T, Id)]]

    def batchInsert(elems: Enumerator[T])(implicit ctx: ExecutionContext): Future[BatchReturn]

    def batchDelete(sel: Query)(implicit ctx: ExecutionContext): Future[BatchReturn]

    def batchUpdate(sel: Query, upd: Update)(implicit ctx: ExecutionContext): Future[BatchReturn]
    */

}