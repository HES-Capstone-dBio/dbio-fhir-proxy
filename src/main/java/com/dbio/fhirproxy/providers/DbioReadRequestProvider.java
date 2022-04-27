package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.dbio.fhirproxy.resources.DbioReadRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class DbioReadRequestProvider implements IResourceProvider {
    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return DbioReadRequest.class
    }

    @Create
    public MethodOutcome createReadRequest(@ResourceParam DbioReadRequest request) {
        return new MethodOutcome();
    }
}
