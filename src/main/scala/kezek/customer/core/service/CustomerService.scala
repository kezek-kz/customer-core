package kezek.customer.core.service

import akka.Done
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import kezek.customer.core.codec.MainCodec
import kezek.customer.core.domain.CustomerFilter._
import kezek.customer.core.domain._
import kezek.customer.core.domain.dto.{CreateCustomerDTO, CustomerListWithTotalDTO, UpdateCustomerDTO}
import kezek.customer.core.exception.ApiException
import kezek.customer.core.repository.CustomerRepository
import kezek.customer.core.repository.mongo.CustomerMongoRepository
import kezek.customer.core.repository.mongo.MongoRepository.DUPLICATED_KEY_ERROR_CODE
import kezek.customer.core.util.SortType
import org.mongodb.scala.{MongoClient, MongoWriteException}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object CustomerService extends MainCodec {

  def generateFilters(firstName: Option[String],
                      lastName: Option[String],
                      email: Option[String],
                      phoneNumber: Option[String]): Seq[CustomerFilter] = {
    var filters: Seq[CustomerFilter] = Seq.empty
    if (firstName.isDefined) filters = filters :+ ByFirstNameFilter(firstName.get)
    if (lastName.isDefined) filters = filters :+ ByLastNameFilter(lastName.get)
    if (email.isDefined) filters = filters :+ ByEmailFilter(email.get)
    if (phoneNumber.isDefined) filters = filters :+ ByPhoneNumberFilter(phoneNumber.get)
    filters
  }

}

class CustomerService()(implicit val mongoClient: MongoClient,
                        implicit val executionContext: ExecutionContext,
                        implicit val system: ActorSystem[_]) extends MainCodec {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val customerRepository: CustomerRepository = new CustomerMongoRepository()

  def paginate(filters: Seq[CustomerFilter],
               page: Option[Int],
               pageSize: Option[Int],
               sortParams: Map[String, SortType]): Future[CustomerListWithTotalDTO] = {
    log.debug(s"paginate() was called {filters: $filters, page: $page, pageSize: $pageSize, sortParams: $sortParams}")
    (for (
      customers <- customerRepository.paginate(filters, page, pageSize, sortParams);
      count <- customerRepository.count(filters)
    ) yield CustomerListWithTotalDTO(
      collection = customers,
      total = count
    )).recover { exception =>
      log.error(s"paginate() failed to paginate customers {exception: $exception, filters: $filters, page: $page, pageSize: $pageSize, sortParams: $sortParams}")
      throw new RuntimeException(s"Failed to paginate customers: $exception")
    }
  }


  def update(id: String, updateCustomerDTO: UpdateCustomerDTO): Future[Customer] = {
    log.debug(s"update() was called {id: $id, updateCustomerDTO: $updateCustomerDTO}")
    val customer = Customer(
      id,
      updateCustomerDTO.firstName,
      updateCustomerDTO.lastName,
      updateCustomerDTO.email,
      updateCustomerDTO.phoneNumber
    )
    customerRepository.update(id, customer)
  }

  def getById(id: String): Future[Customer] = {
    log.debug(s"getById() was called {id: $id}")
    customerRepository.findById(id).map {
      case Some(customer) => customer
      case None =>
        log.error(s"getById() failed to find customer {id: $id}")
        throw ApiException(StatusCodes.NotFound, s"Failed to find customer with id: $id")
    }
  }

  def create(createCustomerDTO: CreateCustomerDTO): Future[Customer] = {
    log.debug(s"create() was called {createCustomerDTO: ${createCustomerDTO.asJson.noSpaces}}")
    val customer = Customer(
      UUID.randomUUID().toString,
      createCustomerDTO.firstName,
      createCustomerDTO.lastName,
      createCustomerDTO.email,
      createCustomerDTO.phoneNumber
    )
    customerRepository.create(customer).recover {
      case ex: MongoWriteException if ex.getCode == DUPLICATED_KEY_ERROR_CODE =>
        log.error(s"create() failed to create customer due to duplicate key {ex: $ex, customer: ${customer.asJson.noSpaces}")
        throw ApiException(StatusCodes.Conflict, s"Failed to create, customer with id: ${customer.id} already exists")
      case ex: Exception =>
        log.error(s"create() failed to create customer {ex: $ex, customer: ${customer.asJson.noSpaces}}")
        throw ApiException(StatusCodes.ServiceUnavailable, ex.getMessage)
    }
  }

  def delete(id: String): Future[Done] = {
    log.debug(s"delete() was called {id: $id}")
    customerRepository.delete(id)
  }
}

