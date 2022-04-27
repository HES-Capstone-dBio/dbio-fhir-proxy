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

import java.util.List;

public class DbioWriteRequest extends AccessControlResource {
    @Override public BaseContainedDt getContained() { return null; }
    @Override public IdDt getId() { return id; }
    @Override public CodeDt getLanguage() { return new CodeDt("EN"); }
    @Override public IBaseMetaType getMeta() { return null; }
    @Override public IIdType getIdElement() { return id; }
    @Override public IBaseResource setId(IIdType iIdType) { return this; }
    @Override public ResourceMetadataMap getResourceMetadata() { return null; }
    @Override public FhirVersionEnum getStructureFhirVersionEnum() { return FhirVersionEnum.R4; }
    @Override public BaseNarrativeDt<?> getText() { return null; }
    @Override public String getResourceName() { return "DbioReadRequest"; }
    @Override public void setId(IdDt idDt) { this.id = idDt; }
    @Override public void setLanguage(CodeDt codeDt) {}
    @Override public void setResourceMetadata(ResourceMetadataMap resourceMetadataMap) {}
    @Override public <T extends IElement> List<T> getAllPopulatedChildElementsOfType(Class<T> aClass) { return null; }
    @Override public boolean isEmpty() { return requestorDetails.isEmpty(); }
    @Override public boolean hasFormatComment() { return false; }
    @Override public List<String> getFormatCommentsPre() { return null; }
    @Override public List<String> getFormatCommentsPost() { return null; }
    @Override public Object getUserData(String s) { return null; }
    @Override public void setUserData(String s, Object o) {}
    @Override public IBaseResource setId(String s) {
        this.id = new IdDt(s);
        return this;
    }
}