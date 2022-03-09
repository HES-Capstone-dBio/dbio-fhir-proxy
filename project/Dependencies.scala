import sbt._

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  val jetty = "org.eclipse.jetty" % "jetty-server" % "11.0.8"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.7"
  val servlet = "javax.servlet" % "servlet-api" % "2.5"

  object hapi {
    val group = "ca.uhn.hapi.fhir" 
    val version = "5.7.0"
    val server = group % "hapi-fhir-server" % version
    val model = group % "hapi-fhir-structures-r4" % version
  }

}
