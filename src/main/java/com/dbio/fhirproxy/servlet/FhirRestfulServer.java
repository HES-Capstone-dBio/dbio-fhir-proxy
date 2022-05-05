package com.dbio.fhirproxy.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.dbio.fhirproxy.providers.DbioAccessRequestProvider;
import com.dbio.fhirproxy.providers.DiagnosticResourceProvider;
import com.dbio.fhirproxy.providers.ImagingStudyProvider;
import com.dbio.fhirproxy.providers.PatientResourceProvider;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

@WebServlet("/*")
public class FhirRestfulServer extends RestfulServer {

    private final ApplicationContext applicationContext;

    public FhirRestfulServer(ApplicationContext context) {
        this.applicationContext = context;
         try {
             initialize();
         } catch (Exception e) {
             System.out.println("Unable to initialize FHIR Proxy");
         };
    }

    @Override
    protected void initialize() throws ServletException {
        super.initialize();
        System.loadLibrary("ironoxide_java"); // Set up IronCore binary
        setFhirContext(FhirContext.forR4());
        setResourceProviders(new DiagnosticResourceProvider(), new ImagingStudyProvider(), new PatientResourceProvider(), new DbioAccessRequestProvider());
    }
}
