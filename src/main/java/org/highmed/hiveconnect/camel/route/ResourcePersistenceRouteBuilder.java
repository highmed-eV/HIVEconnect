package org.highmed.hiveconnect.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.highmed.hiveconnect.exception.HiveConnectExceptionHandler;
import org.springframework.stereotype.Component;


@Component
public class ResourcePersistenceRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route to perform the ETL for the FHIR request
        from("direct:ResourcePersistenceProcessor")
        
            .doTry()
                // Forward request to FHIR server
                .to("direct:FHIRProcess")
            .doCatch(Exception.class)
                .log("direct:FHIRProcess exception")
                .process(new HiveConnectExceptionHandler())
            .endDoTry()
            .end()

            // Add the extracted reference Resources to the input fhir bundle
            .doTry()
                .to("direct:referencedResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:referencedResourceProcessor exception")
                .process(new HiveConnectExceptionHandler())
            .endDoTry()
            .end();

        }
}