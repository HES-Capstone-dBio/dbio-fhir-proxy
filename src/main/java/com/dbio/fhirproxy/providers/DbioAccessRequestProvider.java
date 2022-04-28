package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import cats.effect.IO;
import cats.effect.unsafe.IORuntime;
import com.dbio.fhirproxy.resources.DbioAccessRequest;
import com.dbio.protocol.AccessRequest;
import com.dbio.protocol.AccessRequestStatus;
import com.dbio.protocol.DbioAccessControl;
import com.dbio.protocol.DbioResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.http4s.client.Client;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DbioAccessRequestProvider implements IResourceProvider {
    private static final Tuple2<Client<IO>, IO<BoxedUnit>> clientAllocate = DbioResource.allocateClient().unsafeRunSync(IORuntime.global());

    private static DbioAccessRequest fromAccessRequestStatus(AccessRequestStatus stat) {
        DbioAccessRequest out = new DbioAccessRequest();
        out.isApproved = stat.requestApproved();
        out.isOpen = stat.requestOpen();
        out.requestorEthAddress = stat.requestorEthAddress();
        out.requesteeEthAddress = stat.requesteeEthAddress();
        out.id = new IdDt(stat.id());
        out.requestorDetails = "NO DETAILS YET";
        return out;
    }

    @Override
    protected void finalize() throws Throwable {
        clientAllocate._2().unsafeRunSync(IORuntime.global());
        super.finalize();
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return DbioAccessRequest.class;
    }

    @Create
    public MethodOutcome createAccessRequest(@ResourceParam DbioAccessRequest dbioAccessRequest) {
        AccessRequest req = new AccessRequest(dbioAccessRequest.requestorEthAddress, dbioAccessRequest.requesteeEthAddress);
        try {
            switch (dbioAccessRequest.type) {
                case ReadRequest:
                    DbioAccessControl.postReadRequest(req, clientAllocate._1()).unsafeRunSync(IORuntime.global());
                case WriteRequest:
                    DbioAccessControl.postWriteRequest(req, clientAllocate._1()).unsafeRunSync(IORuntime.global());
            }
        } catch (Throwable e) {
            return new MethodOutcome().setOperationOutcome(
                    ProviderUtils.fhirException(String.format("Create AccessRequest failed with {}", e)));
        }
        return new MethodOutcome(new IdType(ProviderUtils.generateUUID(dbioAccessRequest)), true);
    }

    @Search
    public List<DbioAccessRequest> searchAccessRequests(
            @RequiredParam(name = "requestee_eth_address") String requesteeEthAddress,
            @RequiredParam(name = "type") DbioAccessRequest.AccessControlType type
    ) {
        switch (type) {
            case WriteRequest:
                return ProviderUtils.toJavaList(
                        DbioAccessControl.getReadRequests(requesteeEthAddress, clientAllocate._1())
                                .unsafeRunSync(IORuntime.global())
                ).stream().map(DbioAccessRequestProvider::fromAccessRequestStatus).collect(Collectors.toList());
            case ReadRequest:
                return ProviderUtils.toJavaList(
                        DbioAccessControl.getWriteRequests(requesteeEthAddress, clientAllocate._1())
                                .unsafeRunSync(IORuntime.global())
                ).stream().map(DbioAccessRequestProvider::fromAccessRequestStatus).collect(Collectors.toList());
        }
        return new ArrayList<DbioAccessRequest>();
    }
}
