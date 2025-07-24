package org.highmed.hiveconnect.camel.route;

import org.highmed.hiveconnect.fhir.camel.RequestDetailsLookupProcessor;
import org.highmed.hiveconnect.openehr.camel.ProvideResourceResponseProcessor;
import org.springframework.stereotype.Component;


@Component
public class HiveConnectRouteBuilder extends AbstractRouteBuilder {

    public static final String DIRECT_INPUT_PROCESS = "direct:InputProcess";

    public HiveConnectRouteBuilder() {
        super();
    }

    @Override
    public void configure() throws Exception {


        from("direct:CreateRouteProcess")
            .routeId("CreateRouteProcessRoute")
            // .onCompletion()
            //     .process(ProvideResourceAuditHandler.BEAN_ID)
            // .end()
            .to(DIRECT_INPUT_PROCESS)
            .to("direct:provideResource")
            // Prepare the final output
            .to("direct:OutputProcess")
            .log("CreateRouteProcessRoute completed");


        from("direct:SearchRouteProcess")
            .routeId("SearchRouteProcessRoute")
            .log("SearchRouteProcess not supported");
            // .to("direct:InputProcess")
            // .to("direct:ResourcePersistenceProcessor")

        from("direct:ReadRouteProcess")
            .routeId("ReadRouteProcessRoute")
            .to(DIRECT_INPUT_PROCESS)
            .to("direct:ResourcePersistenceProcessor")
            .log("ReadRouteProcess completed");


        from(DIRECT_INPUT_PROCESS)
            //Extract all RequestDetails information from the input
            .process(RequestDetailsLookupProcessor.BEAN_ID);

        from("direct:OutputProcess")
            // Prepare the final output
            .process(ProvideResourceResponseProcessor.BEAN_ID);

    }       
}

// fhir:apiName/methodName
// fhir://endpoint-prefix/endpoint?[options]
// Endpoint prefix can be one of:
// capabilities
// create
// delete
// history
// load-page
// meta
// operation
// patch
// read
// search
// transaction
// update
// validate
