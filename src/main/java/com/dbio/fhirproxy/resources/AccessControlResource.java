package com.dbio.fhirproxy.resources;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.primitive.IdDt;

public abstract class AccessControlResource implements IResource {
    public IdDt id;
    public String requestorDetails;
}
