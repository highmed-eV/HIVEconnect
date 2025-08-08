package org.highmed.hiveconnect.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.openehr.sdk.util.exception.ClientException;
import org.highmed.hiveconnect.exception.HiveConnectExceptionHandler;
import org.highmed.hiveconnect.exception.OpenEhrClientExceptionHandler;
import org.springframework.stereotype.Component;


@Component
public class SendToCDRRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route to perform the ETL for the FHIR request
        from("direct:send-to-cdr")
        //Perform TL
            .doTry()
                .to("direct:TransformProcess")
                .to("direct:LoadProcess")
            .doCatch(Exception.class)
                .log("direct:SendToCDR exception")
                .process(new HiveConnectExceptionHandler())
            .endDoTry()
            .end();

        //Transform
        from("direct:TransformProcess")
            //Process the openFHIR Input
            .doTry()
                .to("direct:OpenFHIRProcess")
            .doCatch(Exception.class)
                .log("direct:OpenFHIRProcess exception")
                .process(new HiveConnectExceptionHandler())
            .endDoTry()
            .end();

        //Load
        from("direct:LoadProcess")
            //Process the EHR Input
            .doTry()
                //Get the mapped openEHRId if avaialbe else create new ehrId
                .to("direct:patientIdToEhrIdMapperProcess")
                // .wireTap("direct:OpenEHRProcess")
                .to("direct:OpenEHRProcess")
                .log("Load to openEHR complete")
            .doCatch(ClientException.class)
                .log("direct:OpenEHRProcess exception")
                .process(new OpenEhrClientExceptionHandler())
            .endDoTry()
            .end();
    }
}