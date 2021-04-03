package kezek.customer.core.api.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kezek.customer.core.api.http.route.CustomerHttpRoutes

import javax.ws.rs.{GET, Path}

@Path("/api/v1")
trait HttpRoutes
  extends CustomerHttpRoutes {

  val routes: Route =
    pathPrefix("api" / "v1") {
      concat(
        healthcheck,
        customerHttpRoutes
      )
    }

  @GET
  @Operation(
    summary = "health check",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "OK"),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/healthcheck")
  @Tag(name = "Healthcheck")
  def healthcheck: Route = {
    path("healthcheck") { ctx =>
      complete("ok")(ctx)
    }
  }
}
