package kezek.customer.core.api.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import kezek.customer.core.codec.MainCodec
import kezek.customer.core.domain.Customer
import kezek.customer.core.domain.dto.{CreateCustomerDTO, CustomerListWithTotalDTO, UpdateCustomerDTO}
import kezek.customer.core.service.CustomerService
import kezek.customer.core.util.{HttpUtil, SortUtil}
import org.joda.time.DateTime

import javax.ws.rs._
import scala.util.{Failure, Success}

trait CustomerHttpRoutes extends MainCodec {

  val customerService: CustomerService

  def customerHttpRoutes: Route = {
    pathPrefix("customers") {
      concat(
        updateCustomer,
        getCustomerById,
        deleteCustomer,
        paginateCustomers,
        createCustomer
      )
    }
  }

  @GET
  @Operation(
    summary = "Get customer list",
    description = "Get filtered and paginated customer list",
    method = "GET",
    parameters = Array(
      new Parameter(name = "firstName", in = ParameterIn.QUERY, example = "Olzhas"),
      new Parameter(name = "lastName", in = ParameterIn.QUERY, example = "Dairov"),
      new Parameter(name = "email", in = ParameterIn.QUERY, example = "test@test.com"),
      new Parameter(name = "phoneNumber", in = ParameterIn.QUERY, example = "+77777777777"),
      new Parameter(name = "page", in = ParameterIn.QUERY, example = "1"),
      new Parameter(name = "pageSize", in = ParameterIn.QUERY, example = "10"),
      new Parameter(name = "sort", in = ParameterIn.QUERY, example = "+phoneNumber,-firstName")
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[CustomerListWithTotalDTO]),
            mediaType = "application/json",
            examples = Array(new ExampleObject(name = "CustomerListWithTotalDTO", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/customers")
  @Tag(name = "Customers")
  def paginateCustomers: Route = {
    get {
      pathEndOrSingleSlash {
        parameters(
          "firstName".?,
          "lastName".?,
          "email".?,
          "phoneNumber".?,
          "page".as[Int].?,
          "pageSize".as[Int].?,
          "sort".?
        ) {
          (firstName,
           lastName,
           email,
           phoneNumber,
           page,
           pageSize,
           sort) => {
            onComplete {
              customerService.paginate(
                CustomerService.generateFilters(
                  firstName = firstName,
                  lastName = lastName,
                  email = email,
                  phoneNumber = phoneNumber,
                ),
                page,
                pageSize,
                SortUtil.parseSortParams(sort)
              )
            } {
              case Success(result) => complete(result)
              case Failure(exception) => HttpUtil.completeThrowable(exception)
            }
          }
        }
      }
    }
  }

  @GET
  @Operation(
    summary = "Get customer by id",
    description = "Returns a full information about customer by id",
    method = "GET",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Customer]),
            examples = Array(new ExampleObject(name = "Customer", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/customers/{id}")
  @Tag(name = "Customers")
  def getCustomerById: Route = {
    get {
      path(Segment) { id =>
        onComplete(customerService.getById(id)) {
          case Success(result) => complete(result)
          case Failure(exception) => HttpUtil.completeThrowable(exception)
        }
      }
    }
  }

  @POST
  @Operation(
    summary = "Create customer",
    description = "Creates new customer",
    method = "POST",
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[CreateCustomerDTO]),
          mediaType = "application/json",
          examples = Array(
            new ExampleObject(name = "CreateCustomerDTO", value = "")
          )
        )
      ),
      required = true
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Customer]),
            examples = Array(new ExampleObject(name = "Customer", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/customers")
  @Tag(name = "Customers")
  def createCustomer: Route = {
    post {
      pathEndOrSingleSlash {
        entity(as[CreateCustomerDTO]) { body =>
          onComplete(customerService.create(body)) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }

  @PUT
  @Operation(
    summary = "Update customer",
    description = "Updates customer",
    method = "PUT",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
    ),
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[UpdateCustomerDTO]),
          mediaType = "application/json",
          examples = Array(new ExampleObject(name = "UpdateCustomerDTO", value = ""))
        )
      ),
      required = true
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Customer]),
            examples = Array(new ExampleObject(name = "Customer", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/customers/{id}")
  @Tag(name = "Customers")
  def updateCustomer: Route = {
    put {
      path(Segment) { id =>
        entity(as[UpdateCustomerDTO]) { body =>
          onComplete(customerService.update(id, body)) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }

  @DELETE
  @Operation(
    summary = "Deletes customer",
    description = "Deletes customer",
    method = "DELETE",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "OK",
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/customers/{id}")
  @Tag(name = "Customers")
  def deleteCustomer: Route = {
    delete {
      path(Segment) { id =>
        onComplete(customerService.delete(id)) {
          case Success(_) => complete(StatusCodes.NoContent)
          case Failure(exception) => HttpUtil.completeThrowable(exception)
        }
      }
    }
  }

}
