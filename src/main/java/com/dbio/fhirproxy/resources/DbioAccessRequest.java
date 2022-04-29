package com.dbio.fhirproxy.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;


@ResourceDef(name = "DbioAccessRequest")
public class DbioAccessRequest extends DomainResource {
    @Child(name = "requestee_eth_address") public StringType requesteeEthAddress;
    @Child(name = "access_request_type") public StringType accessRequestType;
    @Child(name = "is_approved") public BooleanType isApproved = new BooleanType(false);
    @Child(name = "is_open") public BooleanType isOpen = new BooleanType(true);

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Basic;
    }

    @Override
    public DomainResource copy() {
        DbioAccessRequest out = new DbioAccessRequest();
        out.id = this.id;
        out.requesteeEthAddress = this.requesteeEthAddress;
        out.accessRequestType = this.accessRequestType;
        return out;
    }
}
