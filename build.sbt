import scala.io.Source

name := "customer-core"

version := "0.1"

scalaVersion := "2.13.1"

lazy val akkaVersion      = "2.6.8"
lazy val akkaHttpVersion  = "10.2.1"
lazy val swaggerVersion   = "2.1.2"
lazy val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-protobuf-v3" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  //
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.2.0",
  //
  "javax.ws.rs"                  % "javax.ws.rs-api"       % "2.1.1",
  "com.github.swagger-akka-http" %% "swagger-akka-http"    % "2.0.5",
  "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.1.0",
  "io.swagger.core.v3"           % "swagger-core"          % swaggerVersion,
  "io.swagger.core.v3"           % "swagger-annotations"   % swaggerVersion,
  "io.swagger.core.v3"           % "swagger-models"        % swaggerVersion,
  "io.swagger.core.v3"           % "swagger-jaxrs2"        % swaggerVersion,
  //
  "de.heikoseeberger" %% "akka-http-circe"      % "1.31.0",
  "io.circe"          %% "circe-core"           % circeVersion,
  "io.circe"          %% "circe-generic"        % circeVersion,
  "io.circe"          %% "circe-parser"         % circeVersion,
  "io.circe"          %% "circe-literal"        % circeVersion,
  "io.circe"          %% "circe-generic-extras" % circeVersion,
  "io.circe"          %% "circe-jawn"           % circeVersion,
  "io.circe"          %% "circe-bson"           % "0.4.0",
  "joda-time"         % "joda-time"             % "2.10.5",
  "ch.qos.logback"    % "logback-classic"       % "1.2.3" % Runtime,
  //
)

enablePlugins(JavaAppPackaging)

// heroku deployment configs
herokuAppName in Compile := Map(
  "prod"  -> "prod-kezek-customer-core",
  "dev" -> "dev-kezek-customer-core",
)(sys.props.getOrElse("env", "dev"))

herokuJdkVersion in Compile := "1.8"

val devVars = Map(
  "LOG_LEVEL" -> "DEBUG",
  "MONGO_CONNECTION_STRING" -> "mongodb+srv://admin:wqFerF1zogVVnyAO@cluster0.l04gc.mongodb.net",
  "MONGO_DATABASE" -> "dev_kezek",
  "MONGO_CUSTOMER_COLLECTION" -> "customer",
  "HOST" -> "0.0.0.0",
  "SWAGGER_HOST" -> "",
  "SWAGGER_SCHEMES" -> "https",
  "ENV" -> "dev",
)
val prodVars = Map(
  "LOG_LEVEL" -> "DEBUG",
  "MONGO_CONNECTION_STRING" -> "mongodb+srv://admin:wqFerF1zogVVnyAO@cluster0.l04gc.mongodb.net",
  "MONGO_DATABASE" -> "prod_kezek",
  "MONGO_CUSTOMER_COLLECTION" -> "customer",
  "HOST" -> "0.0.0.0",
  "SWAGGER_HOST" -> "",
  "SWAGGER_SCHEMES" -> "https",
  "ENV" -> "dev",
)

herokuConfigVars in Compile := Map(
  "prod" -> prodVars,
  "dev" -> devVars
)(sys.props.getOrElse("env", "dev"))