import sbt._

object Dependencies {
  lazy val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % "10.2.7"
  lazy val akkaStreams = "com.typesafe.akka" %% "akka-stream" % "2.6.20"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15"
  lazy val testContainers = "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.12"
}
