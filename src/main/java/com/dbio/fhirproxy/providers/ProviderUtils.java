package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProviderUtils {
    private static final IParser parser = FhirContext.forR4().newJsonParser();
    public static String PROVIDER_EMAIL = System.getenv("THIRD_PARTY_EMAIL");
    public static String PASSWORD = System.getenv("THIRD_PARTY_PRIVATE_KEY");
    public static String PROVIDER_ETH_ADDRESS = System.getenv("THIRD_PARTY_ETH_ADDRESS");
    public static String PROVIDER_DETAILS = "Massachusetts General Hospital";

    /**
     * Hash the Patient resource using the MD5 algorithm and truncate to 64 chars.
     */
    public static <R extends DomainResource> String generateUUID(R resource) {
        byte[] bytes = parser.encodeResourceToString(resource).getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(bytes).toString();
    }

    /**
     * Serialize a Resource object to JSON string.
     */
    public static <R extends DomainResource> String serialize(R resource) {
        return parser.encodeResourceToString(resource);
    }

    /**
     * Deserialize a JSON string back to a Resource object.
     */
    public static <R extends DomainResource> R deserialize(Class<R> clazz, String json) {
        return parser.parseResource(clazz, json);
    }

    /**
     * Yield an OperationOutcome which describes an error.
     */
    public static OperationOutcome fhirException(String diagnostic) {
        return new OperationOutcome().addIssue(
                new OperationOutcome.OperationOutcomeIssueComponent().setCode(OperationOutcome.IssueType.EXCEPTION).setDiagnostics(diagnostic));
    }

    public static <A> List<A> toJavaList(scala.collection.immutable.List<A> ls) {
        List<A> out = new ArrayList<A>();
        ls.foreach(out::add);
        return out;
    }

}
