package kezek.customer.core.api.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class HttpRoutes {

  val routes: Route =
    pathPrefix("api" / "v1") {
      concat(
        path("healthcheck") { ctx =>
          complete("ok")(ctx)
        }
      )
    }

}
