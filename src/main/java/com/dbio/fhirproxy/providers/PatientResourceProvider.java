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
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PatientResourceProvider implements IResourceProvider {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final IParser parser = FhirContext.forR4().newJsonParser();
    public static String TYPE_NAME = "Patient";
    // TODO: Put these fields in ENV
    public static String CREATOR_EMAIL = "naa131@g.harvard.edu";
    public static String PASSWORD = "f39d271eff903230d73511104bb0bb4233af0cc77ad18731488e968e885b6f22";
    public static String CREATOR_ETH_ADDRESS = "0xb0c958dB100EFC9DbB725B54e93339d73158Df8a";

    /** Hash the Patient resource using the MD5 algorithm and truncate to 64 chars. */
    private static String hashId(Patient patient) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = parser.encodeResourceToString(patient).getBytes(StandardCharsets.UTF_8);
            return new BigInteger(1, bytes).toString(36).subSequence(0, 64).toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }

    @Search()
    public Patient searchPatient(@RequiredParam(name="id") String id, @RequiredParam(name="subjectEmail") String subjectEmail) {
        DbioGetRequest req = new DbioGetRequest(
                subjectEmail,
                CREATOR_EMAIL,
                PASSWORD,
                TYPE_NAME,
                id);
        log.info(String.format("[DbioResource] Patient GET: %s", req));
        DbioGetResponse response = DbioResource.get(req).unsafeRunSync(IORuntime.global());
        Patient out = parser.parseResource(Patient.class, response.plaintext().noSpaces());;
        return (Patient) out.setId(new IdType(id));
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Patient patient, @RequiredParam(name="subjectEmail") String subjectEmail) {
        String id = hashId(patient);
        DbioPostRequest request = new DbioPostRequest(
                subjectEmail,
                CREATOR_EMAIL,
                PASSWORD,
                CREATOR_ETH_ADDRESS,
                TYPE_NAME,
                id,
                parser.encodeResourceToString(patient)
        );
        try {
            DbioPostResponse response = DbioResource.post(request).unsafeRunSync(IORuntime.global());
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
