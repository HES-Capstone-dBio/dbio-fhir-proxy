package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.rest.annotation.*;
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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;
import org.http4s.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbio.fhirproxy.providers.ProviderUtils.PROVIDER_DETAILS;
import static com.dbio.fhirproxy.providers.ProviderUtils.PROVIDER_ETH_ADDRESS;

public class DbioAccessRequestProvider implements IResourceProvider {
    private static final Tuple2<Client<IO>, IO<BoxedUnit>> clientAllocate = DbioResource.allocateClient().unsafeRunSync(IORuntime.global());
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static String makeRequestId(AccessRequestStatus stat) {
        return String.format("%s-%s", stat.requestType().get(), stat.id());
    }

    private static DbioAccessRequest fromAccessRequestStatus(AccessRequestStatus stat) {
        DbioAccessRequest out = new DbioAccessRequest();
        out.isApproved = new BooleanType(stat.requestApproved());
        out.isOpen = new BooleanType(stat.requestOpen());
        out.requesteeEthAddress = new StringType(stat.requesteeEthAddress());
        out.createdDate = new DateTimeType(stat.createdTime().toString());
        out.updatedDate = new DateTimeType(stat.lastUpdatedTime().toString());
        out.accessRequestType = new StringType(stat.requestType().get());
        out.setId(makeRequestId(stat));
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
        AccessRequest req = new AccessRequest(PROVIDER_ETH_ADDRESS, dbioAccessRequest.requesteeEthAddress.getValue(), PROVIDER_DETAILS);
        switch (dbioAccessRequest.accessRequestType.getValue()) {
            case "ReadRequest":
                log.info(String.format("POST ReadRequest for %s", req.requesteeEthAddress()));
                DbioAccessRequest readOut = fromAccessRequestStatus(DbioAccessControl.postReadRequest(req, clientAllocate._1()).unsafeRunSync(IORuntime.global()));
                return new MethodOutcome().setResource(readOut);
            case "WriteRequest":
                log.info(String.format("POST WriteRequest for %s", req.requesteeEthAddress()));
                DbioAccessRequest writeOut = fromAccessRequestStatus(DbioAccessControl.postWriteRequest(req, clientAllocate._1()).unsafeRunSync(IORuntime.global()));
                return new MethodOutcome().setResource(writeOut);
            default: throw new IllegalArgumentException(String.format("Incorrect type information in request: %s", dbioAccessRequest.accessRequestType));
        }
    }

    @Search
    public List<DbioAccessRequest> searchAccessRequests(@RequiredParam(name = "requestee_eth_address") String requesteeEthAddress, @RequiredParam(name = "access_request_type") String type) {
        switch (type) {
            case "ReadRequest":
                return ProviderUtils.toJavaList(DbioAccessControl.getReadRequests(requesteeEthAddress, clientAllocate._1())
                        .unsafeRunSync(IORuntime.global()))
                        .stream()
                        .map(DbioAccessRequestProvider::fromAccessRequestStatus)
                        .collect(Collectors.toList());
            case "WriteRequest":
                return ProviderUtils.toJavaList(DbioAccessControl.getWriteRequests(requesteeEthAddress, clientAllocate._1())
                                .unsafeRunSync(IORuntime.global()))
                        .stream()
                        .map(DbioAccessRequestProvider::fromAccessRequestStatus)
                        .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Read
    public DbioAccessRequest getAccessRequest(@IdParam IdType id) {
        String[] typeId = id.getValue().split("/")[1].split("-");
        switch (typeId[0]) {
            case "ReadRequest":
                return fromAccessRequestStatus(DbioAccessControl
                        .getReadRequest(Integer.parseInt(typeId[1]), PROVIDER_ETH_ADDRESS, clientAllocate._1())
                        .unsafeRunSync(IORuntime.global()));
            case "WriteRequest":
                return fromAccessRequestStatus(DbioAccessControl
                        .getWriteRequest(Integer.parseInt(typeId[1]), PROVIDER_ETH_ADDRESS, clientAllocate._1())
                        .unsafeRunSync(IORuntime.global()));
            default: throw new IllegalArgumentException(String.format("Incorrect type information in request ID: %s", id));
        }
    }
}
