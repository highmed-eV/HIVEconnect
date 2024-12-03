package org.ehrbase.fhirbridge.config;

import org.apache.camel.CamelContext;
// import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.impl.DefaultCamelContext;
import org.ehrbase.fhirbridge.camel.route.CamelRoute;
import org.ehrbase.fhirbridge.core.PatientIdMapper;
import org.ehrbase.fhirbridge.exception.CamelErrorHandlerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {
    
    private String contextPath;
    private String serverPort;
    private PatientIdMapper patientIdMapper;
    public CamelConfig(PatientIdMapper patientIdMapper,
                        @Value("${camel.servlet.mapping.contextPath}") String contextPath,
                        @Value("${server.port}") String serverPort) {
        this.patientIdMapper = patientIdMapper;
        this.contextPath = contextPath;
        this.serverPort = serverPort;
    }

    // @Bean
    // public CamelContext camelContext() {
    //     CamelContext context = new DefaultCamelContext();
        
    //     try {
    //         context.addRoutes(new CamelRoute(patientIdMapper, contextPath, serverPort));
    //     } catch (Exception e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     }
    //     // Start the context if necessary (sometimes Spring does this automatically)
    //     context.start();
        
    //     return context;
    // }

    // @Bean
    // public ServletRegistrationBean servletRegistrationBean() {
    //     ServletRegistrationBean servlet = new ServletRegistrationBean
    //     (new CamelHttpTransportServlet(), contextPath+"/*");
    //     servlet.setName("CamelServlet");
    //     return servlet;
    // }

    // @Bean
    // public CamelRoute myRouteBuilder() {
    //     return new CamelRoute(null);
    // }

    // // Example custom error handler
    // @Bean
    // public CamelErrorHandlerBuilder errorHandlerBuilder() {
    //     return new CamelErrorHandlerBuilder();
    // }
}
