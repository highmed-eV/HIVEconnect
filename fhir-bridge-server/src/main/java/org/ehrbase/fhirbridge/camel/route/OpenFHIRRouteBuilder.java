package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S1192")
public class OpenFHIRRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        from("direct:OpenFHIRProcess")
            //Convert FHIR request JSON to openEHR format using openFHIR
            .to("bean:fhirBridgeOpenFHIRAdapter?method=convertToOpenEHR")
            .log("FHIR converted to openEHR format.")

            //Store the response in the Exchange
            .process(exchange -> {
                String response = exchange.getIn().getBody(String.class);
                exchange.getMessage().setHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME, response);
            });
    }
}

