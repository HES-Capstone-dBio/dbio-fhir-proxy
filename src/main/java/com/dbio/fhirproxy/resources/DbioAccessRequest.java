package com.dbio.fhirproxy.resources;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.ResourceType;

public class DbioAccessRequest extends DomainResource {
    public IdDt id;
    public String requestorEthAddress;
    public String requesteeEthAddress;
    public String requestorDetails;
    public AccessControlType type;
    public boolean isApproved;
    public boolean isOpen;

    public enum AccessControlType {
        ReadRequest,
        WriteRequest
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Basic;
    }

    @Override
    public DomainResource copy() {
        DbioAccessRequest out = new DbioAccessRequest();
        out.id = this.id;
        out.requestorDetails = this.requestorDetails;
        out.requesteeEthAddress = this.requesteeEthAddress;
        out.requestorEthAddress = this.requestorEthAddress;
        return out;
    }
}
