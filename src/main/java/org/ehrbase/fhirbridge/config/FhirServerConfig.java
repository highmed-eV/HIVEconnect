package org.ehrbase.fhirbridge.config;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.ServletException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HAPI FHIR.
 *
 * @author Mathieu Ouellet
 */
@Configuration
@ConfigurationProperties("hapi.fhir.rest")
public class FhirServerConfig extends RestfulServer {
    private final List<IResourceProvider> resourceProviders;

    private final FhirContext fhirContext;

    private final String serverPath;

    public FhirServerConfig(List<IResourceProvider> resourceProviders, FhirContext fhirContext, @Value("${hapi.fhir.server.path}") String serverPath) {
        this.resourceProviders = resourceProviders;
        this.fhirContext = fhirContext;
        this.serverPath = serverPath;
    }

    @Bean
    public ServletRegistrationBean fhirServerRegistrationBean() {
        ServletRegistrationBean registration = new ServletRegistrationBean(this, serverPath + "/*");
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        setFhirContext(fhirContext);
        setResourceProviders(this.resourceProviders);
        setServerAddressStrategy(new HardcodedServerAddressStrategy(serverPath));

        // registerInterceptor(new ExceptionHandler());
        registerInterceptor(new HttpInterceptor());

        //registerInterceptor(new OpenApiInterceptor());
    }
}