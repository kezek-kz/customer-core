package kezek.customer.core.repository.mongo

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import kezek.customer.core.codec.MainCodec
import kezek.customer.core.domain.CustomerFilter._
import kezek.customer.core.domain.{Customer, CustomerFilter}
import kezek.customer.core.exception.ApiException
import kezek.customer.core.repository.CustomerRepository
import kezek.customer.core.repository.mongo.CustomerMongoRepository.fromFiltersToBson
import kezek.customer.core.util.{PaginationUtil, SortType}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections
import org.mongodb.scala.model.Sorts.{metaTextScore, orderBy}
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CustomerMongoRepository {

  private def fromFiltersToBson(filters: Seq[CustomerFilter]): Bson = {
    if (filters.isEmpty) Document()
    else and(
      filters.map {
        case ByFirstNameFilter(firstName) => equal("firstName", firstName)
        case ByLastNameFilter(lastName) => equal("lastName", lastName)
        case ByEmailFilter(email) => equal("email", email)
        case ByPhoneNumberFilter(phoneNumber) => equal("phoneNumber", phoneNumber)
        case other =>
          throw new RuntimeException(s"Failed to generate bson filter: $other not implemented")
      }: _*
    )
  }

}

class CustomerMongoRepository()(implicit val mongoClient: MongoClient,
                                implicit val executionContext: ExecutionContext)
  extends CustomerRepository with MainCodec with MongoRepository {

  override val sortingFields: Seq[String] = Seq("phoneNumber", "firstName")
  val config: Config = ConfigFactory.load()
  val database: MongoDatabase = mongoClient.getDatabase(config.getString("db.mongo.database"))
  val collection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.customer"))

  override def create(customer: Customer): Future[Customer] = {
    collection.insertOne(toDocument(customer)).head().map(_ => customer)
  }

  private def toDocument(customer: Customer): Document = {
    Document(customer.asJson.noSpaces)
  }

  override def update(id: String, customer: Customer): Future[Customer] = {
    collection.replaceOne(equal("id", id), toDocument(customer)).head().map { updateResult =>
      if (updateResult.wasAcknowledged()) {
        customer
      } else {
        throw new RuntimeException(s"Failed to replace customer with id: $id")
      }
    }
  }

  override def findById(id: String): Future[Option[Customer]] = {
    collection
      .find(equal("id", id))
      .first()
      .headOption()
      .map {
        case Some(document) => Some(fromDocumentToCustomer(document))
        case None => None
      }
  }

  private def fromDocumentToCustomer(document: Document): Customer = {
    parse(document.toJson()).toTry match {
      case Success(json) =>
        json.as[Customer].toTry match {
          case Success(customer) => customer
          case Failure(exception) => throw exception
        }
      case Failure(exception) => throw exception
    }
  }

  override def paginate(filters: Seq[CustomerFilter],
                        page: Option[Int],
                        pageSize: Option[Int],
                        sortParams: Map[String, SortType]): Future[Seq[Customer]] = {
    val filtersBson = fromFiltersToBson(filters)
    val sortBson = orderBy(fromSortParamsToBson(sortParams), metaTextScore("score"))
    val limit = pageSize.getOrElse(10)
    val offset = PaginationUtil.offset(page = page.getOrElse(1), size = limit)

    collection
      .find(filtersBson)
      .projection(Projections.metaTextScore("score"))
      .sort(sortBson)
      .skip(offset)
      .limit(limit)
      .toFuture()
      .map(documents => documents map fromDocumentToCustomer)
  }

  override def count(filters: Seq[CustomerFilter]): Future[Long] = {
    collection.countDocuments(fromFiltersToBson(filters)).head()
  }

  override def delete(id: String): Future[Done] = {
    collection.deleteOne(equal("id", id)).head().map { deleteResult =>
      if (deleteResult.wasAcknowledged() && deleteResult.getDeletedCount == 1) {
        Done
      } else {
        throw ApiException(StatusCodes.NotFound, "Failed to delete customer")
      }
    }
  }
}
