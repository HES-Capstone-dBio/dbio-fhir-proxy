package com.dbio.fhirproxy;

import com.dbio.fhirproxy.servlet.FhirRestfulServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    @Autowired
    private ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ServletRegistrationBean ServetRegistrationBean() {
        ServletRegistrationBean registration= new ServletRegistrationBean(new FhirRestfulServer(context),"/fhir/*");
        registration.setName("FhirServlet");
        return registration;
    }
}
