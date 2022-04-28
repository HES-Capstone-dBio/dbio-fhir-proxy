package com.dbio.fhirproxy.resources;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.ResourceType;

public abstract class AccessControlResource extends DomainResource {
    public IdDt id;
    public String requestorEthAddress;
    public String requesteeEthAddress;
    public String requestorDetails;
    @Override public ResourceType getResourceType() { return ResourceType.Basic; }
}
