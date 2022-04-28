package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import cats.effect.IO;
import cats.effect.unsafe.IORuntime;
import com.dbio.fhirproxy.resources.DbioReadRequest;
import com.dbio.protocol.AccessRequest;
import com.dbio.protocol.DbioAccessControl;
import com.dbio.protocol.DbioResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.http4s.client.Client;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

public class DbioReadRequestProvider implements IResourceProvider {

    private static final IParser parser = FhirContext.forR4().newJsonParser();
    private static Tuple2<Client<IO>, IO<BoxedUnit>> clientAllocate = DbioResource.allocateClient().unsafeRunSync(IORuntime.global());

    OperationOutcome exception = new OperationOutcome().addIssue(
            new OperationOutcome.OperationOutcomeIssueComponent().setCode(OperationOutcome.IssueType.EXCEPTION).setDiagnostics("Something went wrong"));

    @Override
    protected void finalize() throws Throwable {
        clientAllocate._2().unsafeRunSync(IORuntime.global());
        super.finalize();
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return DbioReadRequest.class;
    }

    @Create
    public MethodOutcome createReadRequest(@ResourceParam DbioReadRequest dbioReadRequest) {
        AccessRequest req = new AccessRequest(dbioReadRequest.requestorEthAddress, dbioReadRequest.requesteeEthAddress);
        try {
            DbioAccessControl.postReadRequest(req, clientAllocate._1()).unsafeRunSync(IORuntime.global());
        } catch (Throwable e) {
            return new MethodOutcome().setOperationOutcome(exception);
        }
        return new MethodOutcome(new IdType(""), true);
    }
}
