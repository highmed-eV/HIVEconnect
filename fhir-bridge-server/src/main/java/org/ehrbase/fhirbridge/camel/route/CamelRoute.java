package org.ehrbase.fhirbridge.camel.route;

import ca.uhn.fhir.rest.api.MethodOutcome;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;


@Component
public class CamelRoute extends RouteBuilder {

    private String contextPath;
    private String serverPort;

    public CamelRoute(@Value("${camel.servlet.mapping.contextPath}") String contextPath,
                        @Value("${server.port}") String serverPort) {
        this.contextPath = contextPath;
        this.serverPort = serverPort;
    }

    @Override
    public void configure() throws Exception {

        restConfiguration()
        .contextPath(contextPath) 
        .port(serverPort)
        .apiContextPath("/api-doc")
        .apiProperty("api.title", "Spring Boot Camel Postgres Rest API.")
        .apiProperty("api.version", "1.0")
        .apiProperty("cors", "true")
        .apiContextRouteId("doc-api")
        .component("servlet")
        .bindingMode(RestBindingMode.json);

        // Define REST API for handling FHIR requests
        // from("fhir://transaction/withBundle?serverUrl=http://localhost:8888/fhir-bridge/&fhirVersion={{fhirVersion}}&startScheduler=false&sendEmptyMessageWhenIdle=true&bridgeErrorHandler=true")
        // from("rest:post:fhir?consumes=application/json&produces=application/json")
        // .to("direct:FHIRIBridgeProcess");

        rest("/fhir")
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .post("/")
            .to("direct:FHIRBridgeProcess");
            
            // @formatter:on
            configurePatient();

    }       


    /**
     * Configures available endpoints for {@link org.hl7.fhir.r4.model.Patient Patient} resource.
     */
    private void configurePatient() {
        rest("/fhir/Patient")
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .post("/")
            .outType(MethodOutcome.class)
            .to("direct:FHIRBridgeProcess");

        // from("patient-find:patientEndpoint?fhirContext=#fhirContext&lazyLoadBundles=true")
        //         .process(ResourcePersistenceProcessor.BEAN_ID);
    }

// @formatter:on
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
