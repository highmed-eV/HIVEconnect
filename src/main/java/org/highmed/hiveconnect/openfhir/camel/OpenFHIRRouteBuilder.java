package org.highmed.hiveconnect.openfhir.camel;

import org.apache.camel.builder.RouteBuilder;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.highmed.hiveconnect.exception.OpenFHIRMappingExceptionHandler;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S1192")
public class OpenFHIRRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:validateOpenFHIRProfilesProcess")
        //Validate the incoming profile is supported by openFHIR
        .doTry()
            .to("bean:hiveConnectOpenFHIRAdapter?method=checkProfileSupported")
        .doCatch(RuntimeException.class)
            .log("validateOpenFHIRProfilesProcess: exception")
            .process(new OpenFHIRMappingExceptionHandler())
        .end();

        from("direct:OpenFHIRProcess")
            //Convert FHIR request JSON to openEHR format using openFHIR
            .doTry()
                .to("bean:hiveConnectOpenFHIRAdapter?method=convertToOpenEHR")
            .doCatch(RuntimeException.class)
                .log("OpenFHIRProcess:hiveConnectOpenFHIRAdapter exception")
                .process(new OpenFHIRMappingExceptionHandler())
            .end()
            .log("FHIR converted to openEHR format.")

            //Store the response in the Exchange
            .process(exchange -> {
                String response = exchange.getIn().getBody(String.class);
                exchange.getMessage().setHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME, response);
            });
    }
}

