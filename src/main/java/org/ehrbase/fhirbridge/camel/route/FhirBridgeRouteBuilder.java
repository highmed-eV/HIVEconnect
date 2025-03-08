package org.ehrbase.fhirbridge.camel.route;

import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class FhirBridgeRouteBuilder extends AbstractRouteBuilder {

    private String contextPath;
    private String serverPort;

    public FhirBridgeRouteBuilder(@Value("${camel.servlet.mapping.contextPath}") String contextPath,
                        @Value("${server.port}") String serverPort) {
        super();
        this.contextPath = contextPath;
        this.serverPort = serverPort;
    }

    @Override
    public void configure() throws Exception {


        from("direct:CreateRouteProcess")
            .routeId("CreateRouteProcessRoute")
            // .onCompletion()
            //     .process(ProvideResourceAuditHandler.BEAN_ID)
            // .end()
            .to("direct:provideResource")
            // Prepare the final output 
            .to("direct:OutputProcess")
            .log("CreateRouteProcessRoute completed");
            

        from("direct:SearchRouteProcess")
            .routeId("SearchRouteProcessRoute")
            .log("##########SearchRouteProcess")
            .to("direct:ResourcePersistenceProcessor")
            // Prepare the final output 
            .to("direct:OutputProcess")
            .log("SearchRouteProcess completed");

        from("direct:ReadRouteProcess")
            .routeId("ReadRouteProcessRoute")
            .log("##########ReadRouteProcess");

        from("direct:OutputProcess")
            //Step 9: Prepare the final output
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
