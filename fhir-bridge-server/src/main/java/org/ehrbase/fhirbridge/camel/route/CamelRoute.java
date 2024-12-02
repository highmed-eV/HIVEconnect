package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.util.ObjectHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.config.security.Authenticator;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.core.PatientIdMapper;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.exception.OpenEhrClientExceptionHandler;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.ehrbase.fhirbridge.openehr.camel.EhrLookupProcessor;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class CamelRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CamelRoute.class);

    private String contextPath;
    private String serverPort;
    private PatientIdMapper patientIdMapper;
    private final Authenticator authenticator;

    public CamelRoute(PatientIdMapper patientIdMapper,
                        @Value("${camel.servlet.mapping.contextPath}") String contextPath,
                        @Value("${server.port}") String serverPort,
                        Authenticator authenticator) {
        this.patientIdMapper = patientIdMapper;
        this.contextPath = contextPath;
        this.serverPort = serverPort;
        this.authenticator = authenticator;
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

        // onException(Exception.class)
        //     .handled(true)      
        //     .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        //     .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
        //     .setBody(exceptionMessage())
        //     .useOriginalBody()
        //     .log("######### onException")
        //     ;

        // Define REST API for handling FHIR requests
        // from("fhir://transaction/withBundle?serverUrl=http://localhost:8888/fhir-bridge/&fhirVersion={{fhirVersion}}&startScheduler=false&sendEmptyMessageWhenIdle=true&bridgeErrorHandler=true")
        // from("rest:post:fhir?consumes=application/json&produces=application/json")
        // .to("direct:FHIRIBridgeProcess");


        rest("/fhir")
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .post("/")
            .to("direct:FHIRIBridgeProcess");
            
        from("direct:processAuthentication")
            .log("######### in processAuthentication")
            .process(exchange -> {
                String authHeader = exchange.getIn().getHeader("Authorization", String.class);
                if (!authenticator.authenticate(authHeader, null)) {
                    throw new SecurityException("Unauthorized");
                }
                String body = exchange.getIn().getBody(String.class);
                // set back the request body
                exchange.getMessage().setBody(body);
            })
            .to("direct:FHIRIBridgeProcess");
        
        // Route to process the FHIR request
        from("direct:FHIRIBridgeProcess")
            .marshal().json()
            .convertBodyTo(String.class)
            .log("##########RFHIRIBridgeProcess")
            .process(exchange -> {
                    if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                        String inputResource = (String) exchange.getIn().getBody();
                        String inputResourceType = (String) FhirUtils.getResourceType(inputResource);
                        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, inputResource);
                        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE_TYPE, inputResourceType);
                    }
                })
            .log("FHIR Resource Type ${header.CamelFhirBridgeIncomingResourceType}")
            .to("direct:FHIRInputPreProcess");

        from("direct:FHIRInputPreProcess")
            // Step 1: Extract Patient Id from the FHIR Input Resource
            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} != 'Patient'"))
                // .doTry()
                    .to("direct:patientReferenceProcessor")
                    .log("FHIR Patient ID ${exchangeProperty.FHIRPatientId}")
                // .doCatch(Exception.class)
                //     .process(new FhirBridgeExceptionHandler())
                .end()
            // .endChoice()
            

            // Step 2: Forward request to FHIR server
            // .doTry()
                .to("direct:FHIRProcess")
            // .doCatch(Exception.class)
                // .process(new FhirBridgeExceptionHandler())
            // marshall to JSON for logging
            // .marshal().fhirJson("{{fhirVersion}}")
            // .log("Inserting Patient: ${body}")

            // Step 3: Extract Patient Id created in the FHIR server
            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} != 'Patient'"))
                .to("direct:patientReferencePostProcessor")
                .log("FHIR Patient ID ${exchangeProperty.FHIRPatientId}")
            .end()

            // Process the openFHIR Input 
            .doTry()
                .to("direct:OpenFHIRProcess")
            .doCatch(Exception.class)
                .process(new FhirBridgeExceptionHandler())

            // Process the EHR Input 
            .doTry()
                // .wireTap("direct:OpenEHRProcess")
                .to("direct:OpenEHRProcess")
            .doCatch(ClientException.class)
                .process(new OpenEhrClientExceptionHandler())
            .process(ProvideResourceResponseProcessor.BEAN_ID);
            
            // @formatter:on
            configurePatient();

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


    /**
     * Configures available endpoints for {@link org.hl7.fhir.r4.model.Patient Patient} resource.
     */
    private void configurePatient() {
        rest("/fhir/Patient")
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .post("/")
            .to("direct:FHIRIBridgeProcess");

        // from("patient-find:patientEndpoint?fhirContext=#fhirContext&lazyLoadBundles=true")
        //         .process(ResourcePersistenceProcessor.BEAN_ID);
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
