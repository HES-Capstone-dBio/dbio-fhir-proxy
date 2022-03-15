package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;

/**
 * Author: Stephen Sheldon
 **/
public class PatientResourceProvider implements IResourceProvider {

    /**
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }

    /**
     * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
     *
     * Read operations take a single parameter annotated with the {@link IdParam} paramater, and should return a single resource instance.
     *
     * @param theId
     *            The read operation takes one parameter, which must be of type IdType and must be annotated with the "@Read.IdParam" annotation.
     * @return Returns a resource matching this identifier, or null if none exists.
     */
    @Read()
    public Patient readPatient(@IdParam IdType theId) {
        Patient retVal = new Patient();

        // ...populate...
        retVal.addIdentifier().setSystem("urn:mrns").setValue("12345");
        retVal.addName().setFamily("Smith").addGiven("Sally").addGiven("Sal");
        // ...etc...

        // if you know the version ID of the resource, you should set it and HAPI will
        // include it in a Content-Location header
        retVal.setId(new IdType("Patient", "123", "2"));

        return retVal;
    }
}
