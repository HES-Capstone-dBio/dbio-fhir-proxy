package com.dbio.fhirproxy.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.dbio.fhirproxy.providers.DbioAccessRequestProvider;
import com.dbio.fhirproxy.providers.PatientResourceProvider;
import com.dbio.fhirproxy.providers.ProviderUtils;
import com.dbio.fhirproxy.resources.DbioAccessRequest;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

@WebServlet("/*")
public class FhirRestfulServer extends RestfulServer {

    private final ApplicationContext applicationContext;

    public FhirRestfulServer(ApplicationContext context) {
        this.applicationContext = context;
    }

    @Override
    protected void initialize() throws ServletException {
        super.initialize();
        DbioAccessRequest fart = new DbioAccessRequest();
        fart.requesteeEthAddress = new StringType("sdlfkjsldkfjlksdjf");
        fart.isApproved = new BooleanType(false);
        fart.isOpen = new BooleanType(false);
        System.out.println(ProviderUtils.serialize(fart));
        setFhirContext(FhirContext.forR4());
        setResourceProviders(new PatientResourceProvider(), new DbioAccessRequestProvider());
    }
}
