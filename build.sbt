import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "10.2.7-SNAPSHOT"
ThisBuild / organization     := "io.github.rorp"
ThisBuild / organizationName := "io.github.rorp"

lazy val root = (project in file("."))
  .settings(
    name := "akka-http-socks5",
    libraryDependencies += akkaHttpCore % Provided,
    libraryDependencies += akkaStreams % Provided,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += testContainers % Test,
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
