Global / onChangedBuildSource := ReloadOnSourceChanges
Global / semanticdbEnabled := true

ThisBuild / organization := "com.dbio"
ThisBuild / scalaVersion := "2.12.15"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

Compile / console / scalacOptions --= Seq("-Xfatal-warnings", "-Ywarn-unused")

lazy val root = (project in file(".")).settings(
  name := "protocol-client",
  scalacOptions ++= Seq("-Ywarn-unused", "-Ypartial-unification", "-Xfatal-warnings"),
  resolvers += Resolver.sonatypeRepo("releases"),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.3.11",
    "org.typelevel" %% "cats-effect-kernel" % "3.3.11",
    "org.typelevel" %% "cats-effect-std" % "3.3.11",
    "com.ironcorelabs" %% "ironoxide-scala" % "0.15.0",
    "com.github.jwt-scala" %% "jwt-circe" % "9.0.5",
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
)
