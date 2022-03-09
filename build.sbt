import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "dBio"

lazy val root = (project in file("."))
  .settings(
    name := "dBio-fhir-proxy",
    libraryDependencies ++= Seq(hapi.server, hapi.model, servlet, jetty, catsEffect, scalaTest % Test)
  )
