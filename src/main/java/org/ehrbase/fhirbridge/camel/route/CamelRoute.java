package org.ehrbase.fhirbridge.camel.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class CamelRoute extends AbstractRouteBuilder {

    private String contextPath;
    private String serverPort;

    public CamelRoute(@Value("${camel.servlet.mapping.contextPath}") String contextPath,
                        @Value("${server.port}") String serverPort) {
        super();
        this.contextPath = contextPath;
        this.serverPort = serverPort;
    }

    @Override
    public void configure() throws Exception {


            from("direct:CamelCreateRouteProcess")
            .routeId("CamelCreateRouteProcessRoute")
            // .onCompletion()
            //     .process(ProvideResourceAuditHandler.BEAN_ID)
            // .end()
            .to("direct:FHIRBridgeETLProcess");

            from("direct:CamelSearchRouteProcess")
                .routeId("CamelSearchRouteProcessRoute")
                .log("##########CamelSearchRouteProcess");

            from("direct:CamelReadRouteProcess")
                .routeId("CamelReadRouteProcessRoute")
                .log("##########CamelReadRouteProcess");

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
