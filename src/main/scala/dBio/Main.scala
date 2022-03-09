package dBio

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import ca.uhn.fhir.rest.server.RestfulServer
import ca.uhn.fhir.rest.server.IResourceProvider
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.instance.model.api.IBaseResource
import ca.uhn.fhir.rest.client.exceptions.FhirClientInappropriateForServerException
import ca.uhn.fhir.context.FhirContext

object Main extends IOApp {

  class PatientProvider extends IResourceProvider {
    def getResourceType(): Class[_ <: IBaseResource] = classOf[Patient]
  }

  def run(args: List[String]): IO[ExitCode] = {
    val server = new RestfulServer() { s =>
      s.setFhirContext(FhirContext.forR4())
      s.registerProvider(new PatientProvider)
    }
    IO { server.init() } as ExitCode.Success
  }
}
