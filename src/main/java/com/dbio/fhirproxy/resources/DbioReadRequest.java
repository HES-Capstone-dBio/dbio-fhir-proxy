package com.dbio.fhirproxy.resources;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IElement;
import ca.uhn.fhir.model.base.composite.BaseContainedDt;
import ca.uhn.fhir.model.base.composite.BaseNarrativeDt;
import ca.uhn.fhir.model.base.resource.ResourceMetadataMap;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Narrative;

import java.util.List;

public class DbioReadRequest extends AccessControlResource {
    @Override
    public DomainResource copy() {
        DbioReadRequest out = new DbioReadRequest();
        out.id = this.id;
        out.requestorDetails = this.requestorDetails;
        out.requesteeEthAddress = this.requesteeEthAddress;
        out.requestorEthAddress = this.requestorEthAddress;
        return out;
    }
}