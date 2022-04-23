package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import cats.effect.unsafe.IORuntime;
import com.dbio.protocol.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;

public class PatientResourceProvider implements IResourceProvider {

    public static String TYPE_NAME = "Patient";
    public static String CREATOR_EMAIL = "naa131@g.harvard.edu";
    public static String PASSWORD = "password";
    public static String CREATOR_ETH_ADDRESS = "lksjdflkjsdf";


    /**
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }

    /**
     * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
     * <p>
     * Read operations take a single parameter annotated with the {@link IdParam} parameter, and should return a single resource instance.
     *
     * @param theId The read operation takes one parameter, which must be of type IdType and must be annotated with the "@Read.IdParam" annotation. @return Returns a resource matching this identifier, or null if none exists.
     */
    @Read()
    public Patient readPatient(@IdParam IdType theId, String subjectEmail) {

        IParser parser = FhirContext.forR4().newJsonParser();

        DbioGetRequest req = new DbioGetRequest(
                subjectEmail,
                CREATOR_EMAIL,
                PASSWORD,
                TYPE_NAME,
                theId.toString());

        DbioGetResponse response = DbioResource.get(req).unsafeRunSync(IORuntime.global());
        Patient out = parser.parseResource(Patient.class, response.plaintext().noSpaces());
        return out;
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Patient patient, @ConditionalUrlParam String subjectEmail) {
        IParser parser = FhirContext.forR4().newJsonParser();
        IdType id = IdType.of(patient);
        String plaintext = parser.encodeResourceToString(patient);
        DbioPostRequest request = new DbioPostRequest(
                subjectEmail,
                CREATOR_EMAIL,
                PASSWORD,
                CREATOR_ETH_ADDRESS,
                TYPE_NAME,
                id.getId(),
                parser.encodeResourceToString(patient)
        );
        try {
            DbioPostResponse response = DbioResource.post(request).unsafeRunSync(IORuntime.global());
            return new MethodOutcome(id, new OperationOutcome());
        } catch (Throwable t) {
            String diagnostic = String.format("Patient POST failed with {}", t);
            OperationOutcome.OperationOutcomeIssueComponent issue =
                    new OperationOutcome.OperationOutcomeIssueComponent().setDiagnostics(diagnostic);
            OperationOutcome outcome = new OperationOutcome().addIssue(issue);
            return new MethodOutcome(id, outcome);
        }
    }

}
