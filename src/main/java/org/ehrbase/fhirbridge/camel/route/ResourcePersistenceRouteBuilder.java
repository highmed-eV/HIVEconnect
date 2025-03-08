package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.springframework.stereotype.Component;


@Component
public class ResourcePersistenceRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route to perform the ETL for the FHIR request
        from("direct:ResourcePersistenceProcessor")
        
            .doTry()
                // Step 4: Forward request to FHIR server
                .to("direct:FHIRProcess")
            .doCatch(Exception.class)
                .log("direct:FHIRProcess exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()

            // Step 6: Add the extracted reference Resources to the input fhir bundle
            .doTry()
                .to("direct:referencedResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:referencedResourceProcessor exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end();

        }
}