package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.fhirbridge.openehr.camel.EhrLookupProcessor;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S1192")
public class OpenFHIRRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        from("direct:OpenFHIRProcess")
            // Step 4: Convert FHIR request JSON to openEHR format using openFHIR
            .to("bean:fhirBridgeOpenFHIRAdapter?method=convertToOpenEHR")
            .log("FHIR converted to openEHR format.");
    }
}

