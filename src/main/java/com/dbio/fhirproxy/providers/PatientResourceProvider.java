package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import cats.effect.IO;
import cats.effect.unsafe.IORuntime;
import com.dbio.protocol.*;
import ironoxide.v1.IronOxide;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.http4s.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class PatientResourceProvider implements IResourceProvider {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final IParser parser = FhirContext.forR4().newJsonParser();
    public static String TYPE_NAME = "Patient";
    private static String CREATOR_EMAIL = System.getenv("THIRD_PARTY_EMAIL");
    private static String PASSWORD = System.getenv("THIRD_PARTY_PRIVATE_KEY");
    private static String CREATOR_ETH_ADDRESS = System.getenv("THIRD_PARTY_ETH_ADDRESS");
    private static IronOxide<IO> ironCore = IronCore.forUser(CREATOR_EMAIL, PASSWORD).unsafeRunSync(IORuntime.global());
    private static Tuple2<Client<IO>, IO<BoxedUnit>> clientAllocate = DbioResource.allocateClient().unsafeRunSync(IORuntime.global());
    private static InjectClients injectClients = new InjectClients(ironCore, clientAllocate._1());

    @Override
    protected void finalize() throws Throwable {
        clientAllocate._2.unsafeRunSync(IORuntime.global()); // Close the Client pool
        super.finalize();
    }

    /**
     * Hash the Patient resource using the MD5 algorithm and truncate to 64 chars.
     */
    private static String hashId(Patient patient) {
        byte[] bytes = parser.encodeResourceToString(patient).getBytes(StandardCharsets.UTF_8);
        return new BigInteger(1, bytes).toString(36).subSequence(0, 64).toString();
    }

    /**
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }

    @Search()
    public Patient searchPatient(@RequiredParam(name = "id") String id, @RequiredParam(name = "subjectEmail") String subjectEmail) {
        DbioGetRequest req = new DbioGetRequest(
                subjectEmail,
                CREATOR_EMAIL,
                TYPE_NAME,
                id);
        log.info(String.format("[DbioResource] Patient GET: %s", req));
        DbioGetResponse response = (DbioGetResponse) DbioResource.get(req).apply(injectClients).unsafeRunSync(IORuntime.global());
        Patient out = parser.parseResource(Patient.class, response.plaintext().noSpaces());
        ;
        return (Patient) out.setId(new IdType(id));
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Patient patient, @RequiredParam(name = "subjectEmail") String subjectEmail) {
        String id = hashId(patient);
        DbioPostRequest request = new DbioPostRequest(
                subjectEmail,
                CREATOR_EMAIL,
                CREATOR_ETH_ADDRESS,
                TYPE_NAME,
                id,
                parser.encodeResourceToString(patient)
        );
        try {
            DbioPostResponse response = (DbioPostResponse) DbioResource.post(request).apply(injectClients).unsafeRunSync(IORuntime.global());
            log.info(String.format("[DbioResource] Patient POST succeeded for id: %s", id));
            return new MethodOutcome(new IdType(id), new OperationOutcome());
        } catch (Throwable t) {
            String diagnostic = String.format("[DbioResource] Patient POST failed with: %s", t);
            log.error(diagnostic);
            OperationOutcome.OperationOutcomeIssueComponent issue =
                    new OperationOutcome.OperationOutcomeIssueComponent().setDiagnostics(diagnostic);
            OperationOutcome outcome = new OperationOutcome().addIssue(issue);
            return new MethodOutcome(new IdType(id), outcome); // TODO: Add id to response somehow
        }
    }

}
