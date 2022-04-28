package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.DomainResource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ProviderUtils {
    private static final IParser parser = FhirContext.forR4().newJsonParser();

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

}
