package com.dbio.fhirproxy.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import cats.effect.IO;
import cats.effect.unsafe.IORuntime;
import com.dbio.protocol.*;
import ironoxide.v1.IronOxide;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.http4s.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

import static com.dbio.fhirproxy.providers.ProviderUtils.*;

public class ImagingStudyProvider implements IResourceProvider {
    public static String TYPE_NAME = "ImagingStudy";
    private static final IronOxide<IO> ironCore = IronCore.forUser(PROVIDER_EMAIL, PASSWORD).unsafeRunSync(IORuntime.global());
    private static final Tuple2<Client<IO>, IO<BoxedUnit>> clientAllocate = DbioResource.allocateClient().unsafeRunSync(IORuntime.global());
    private static final InjectClients injectClients = new InjectClients(ironCore, clientAllocate._1());
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void finalize() throws Throwable {
        clientAllocate._2.unsafeRunSync(IORuntime.global()); // Close the Client pool
        super.finalize();
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() { return ImagingStudy.class; }

    @Search()
    public ImagingStudy searchImagingStudy(@RequiredParam(name = "id") String id, @RequiredParam(name = "subjectEmail") String subjectEmail) {
        DbioGetRequest req = new DbioGetRequest(subjectEmail, PROVIDER_EMAIL, TYPE_NAME, id);
        log.info(String.format("[DbioResource] ImagingStudy GET: %s", req));
        DbioGetResponse response = (DbioGetResponse) DbioResource.get(req).apply(injectClients).unsafeRunSync(IORuntime.global());
        ImagingStudy out = ProviderUtils.deserialize(ImagingStudy.class, response.plaintext().noSpaces());
        return (ImagingStudy) out.setId(new IdType(id));
    }

    @Create
    public MethodOutcome createImagingStudy(@ResourceParam ImagingStudy study, @RequiredParam(name = "subjectEmail") String subjectEmail) {
        if (subjectEmail == null){
            throw new IllegalArgumentException("Request must contain query parameter `subjectEmail`");
        }
        String id = ProviderUtils.generateUUID(study);
        DbioPostRequest request = new DbioPostRequest(subjectEmail, PROVIDER_EMAIL, PROVIDER_ETH_ADDRESS, TYPE_NAME, id, ProviderUtils.serialize(study));
        DbioPostResponse response = (DbioPostResponse) DbioResource.post(request).apply(injectClients).unsafeRunSync(IORuntime.global());
        log.info(String.format("[DbioResource] ImagingStudy POST succeeded for id: %s", id));
        return new MethodOutcome(new IdType(id), new OperationOutcome()).setResource(study.setId(new IdType(id)));
    }
}
