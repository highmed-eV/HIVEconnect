package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.util.ObjectHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.ehrbase.fhirbridge.core.PatientIdMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class CamelRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CamelRoute.class);

    private String contextPath;
    private String serverPort;
    private PatientIdMapper patientIdMapper;

    public CamelRoute(PatientIdMapper patientIdMapper,
                        @Value("${camel.servlet.mapping.contextPath}") String contextPath,
                        @Value("${server.port}") String serverPort) {
        this.patientIdMapper = patientIdMapper;
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
        // .component("servlet")
        .bindingMode(RestBindingMode.json);

        // Define REST API for handling FHIR requests
        rest("/fhir")
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .post("/")
            .to("direct:processFHIR");
            
        // Route to process the FHIR request
        from("direct:processFHIR")
            .marshal().json()
            .convertBodyTo(String.class)
            .process(exchange -> {
                    if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                        String inputResource = (String) exchange.getIn().getBody();
                        exchange.getIn().setHeader("CamelFhirBridgeIncomingResource", inputResource);
                    }
                })

            // Step 1: Forward request to HAPI FHIR server
            .choice()
                .when().jsonpath("$[?(@.type == 'transaction')]")
                    //if body.type == "transaction"
                    // create Transaction bundle in our FHIR server
                    .log("Transaction FHIR request. Starting process...")
                    .to("fhir://transaction/withBundle?inBody=stringBundle&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
            .otherwise()
                // else create Resource in our FHIR server
                .log("Resource FHIR request. Starting process...")
                .to("fhir://create/resource?inBody=resourceAsString&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
            .end()
            .log("FHIR request processed by HAPI FHIR server ${body}")

            // Step 2: Extract patient ID from the FHIR request and map it to EHR ID
            .process(exchange -> {
                String fhirRequestJson = exchange.getIn().getBody(String.class);
                String fhirPatientId = extractPatientIdFromFHIR(fhirRequestJson);
                String ehrId = patientIdMapper.mapFhirPatientIdToEhrId(fhirPatientId);
                exchange.getIn().setHeader("EHRId", ehrId); // Store EHRId in the exchange header
            })
            .log("Patient ID mapped to EHR ID: ${header.EHRId}")

            // Step 3: Convert FHIR request JSON to openEHR format using openFHIR
            .to("bean:fhirBridgeOpenFHIRAdapter?method=convertToOpenEHR")
            .log("FHIR converted to openEHR format.")

            // Step 4: Commit the openEHR composition to the openEHR server
            .process(exchange -> {
                String openEhrJson = exchange.getIn().getBody(String.class);
                String ehrId = exchange.getIn().getHeader("EHRId", String.class); // Retrieve EHRId from exchange header
                // Pass the EHRId with the openEHR composition
                exchange.getIn().setBody(openEhrJson);
                exchange.getIn().setHeader("EHRId", ehrId);
            })
            // .to("bean:fhirBridgeOpenEHRAdapter?method=commitComposition")
            .log("Successfully committed composition to openEHR server with EHR ID: ${header.EHRId}");
    }

    // Dummy method for extracting patientId from FHIR JSON
    private String extractPatientIdFromFHIR(String fhirJson) {
        // Implement actual logic to extract the patientId from the FHIR JSON
        // logger.info("extractPatientIdFromFHIR: fhirJson" + fhirJson);
        return "example-patient-id";
    }
}
