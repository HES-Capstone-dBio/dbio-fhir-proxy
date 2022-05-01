package com.dbio.fhirproxy.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.*;

@ResourceDef(name = "DbioAccessRequest")
public class DbioAccessRequest extends DomainResource {
    @Child(name = "requestee_eth_address") public StringType requesteeEthAddress;
    @Child(name = "access_request_type") public StringType accessRequestType;
    @Child(name = "is_approved") public BooleanType isApproved = new BooleanType(false);
    @Child(name = "is_open") public BooleanType isOpen = new BooleanType(true);
    @Child(name = "created_date") public DateTimeType createdDate;

    @Child(name = "last_updated_date") public DateTimeType updatedDate;

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
        out.isApproved = this.isApproved;
        out.isOpen = this.isOpen;
        out.createdDate = this.createdDate;
        out.updatedDate = this.updatedDate;
        return out;
    }
}
