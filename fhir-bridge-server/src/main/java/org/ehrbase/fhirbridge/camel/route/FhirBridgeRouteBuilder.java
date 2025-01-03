package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.config.security.Authenticator;
import org.ehrbase.fhirbridge.core.PatientIdMapper;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.exception.OpenEhrClientExceptionHandler;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class FhirBridgeRouteBuilder extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(FhirBridgeRouteBuilder.class);

    private PatientIdMapper patientIdMapper;
    private final Authenticator authenticator;

    public FhirBridgeRouteBuilder(PatientIdMapper patientIdMapper,
                        Authenticator authenticator) {
        this.patientIdMapper = patientIdMapper;
        this.authenticator = authenticator;
    }

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)      
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
            .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
            .setBody(exceptionMessage())
            // .useOriginalBody()
            .log("######### onException")
            ;


        // from("direct:processAuthentication")
        //     .log("######### in processAuthentication")
        //     .process(exchange -> {
        //         String authHeader = exchange.getIn().getHeader("Authorization", String.class);
        //         if (!authenticator.authenticate(authHeader, null)) {
        //             throw new SecurityException("Unauthorized");
        //         }
        //         String body = exchange.getIn().getBody(String.class);
        //         // set back the request body
        //         exchange.getMessage().setBody(body);
        //     })
        //     .doTry()
        //         .to("direct:FHIRIBridgeProcess")
        //     .doCatch(Exception.class)
        //         .log("direct:FHIRIBridgeProcess catch exception")
        //         .process(new OpenEhrClientExceptionHandler());
    
        // Route to process the FHIR request
        from("direct:FHIRBridgeProcess")
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
            .doTry()
                .to("direct:FHIRToOpenEHRMappingProcess")
            .doCatch(UnprocessableEntityException.class)
                .log("direct:FHIRToOpenEHRMappingProcess catch exception")
                .process(new OpenEhrClientExceptionHandler())
            .end();


        from("direct:FHIRToOpenEHRMappingProcess")
            // Step 1: Extract Patient Id from the FHIR Input Resource
            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} != 'Patient'"))
                    .doTry()
                        .to("direct:extractAndCheckPatientIdExistsProcessor")
                        .log("FHIR Patient ID  ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                    .doCatch(Exception.class)
                        .process(new FhirBridgeExceptionHandler())
                .endChoice()
                .otherwise()
                    .doTry()
                        .to("direct:extractPatientIdFromPatientProcessor")
                        .log("FHIR Patient ID  ${header." + CamelConstants.PATIENT_ID + "}")
                    .doCatch(Exception.class)
                        .process(new FhirBridgeExceptionHandler())
                    .end()
                .endChoice()

            // 2. Extract the input reference Resources from the input fhir bundle
            .doTry()
                .to("direct:mapInternalResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:OpenFHIRProcess catch exception")
                .process(new FhirBridgeExceptionHandler())
            .end()


            .doTry()
                // Step 3: Forward request to FHIR server
                .to("direct:FHIRProcess")
                // Step 4: Extract Patient Id created in the FHIR server
                .to("direct:extractPatientIdFromFhirResponseProcessor")

            .doCatch(Exception.class)
                .log("direct:FHIRProcess catch exception")
                .process(new FhirBridgeExceptionHandler())
            .end()
            // marshall to JSON for logging
            // .marshal().fhirJson("{{fhirVersion}}")
            // .log("Inserting Patient: ${body}")

            // Step 5: Add the extracted reference Resources to the input fhir bundle
            .doTry()
                .to("direct:resourceReferenceProcessor")
            .doCatch(Exception.class)
                .log("direct:resourceReferenceProcessor catch exception")
                .process(new FhirBridgeExceptionHandler())
            .end()

            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} == 'Patient'"))
                    //Get the mapped openEHRId if available else create new ehrId
                    .to("direct:patientIdToEhrIdMapperProcess")
                    .log("Patient ID mapped to EHR ID: ${header.CamelEhrCompositionEhrId}")
                    // Prepare the final output 
                    .process(ProvideResourceResponseProcessor.BEAN_ID)
                .otherwise()
                    //Step 6: Process the openFHIR Input
                    .doTry()
                        .to("direct:OpenFHIRProcess")
                    .doCatch(Exception.class)
                        .log("direct:OpenFHIRProcess catch exception")
                        .process(new FhirBridgeExceptionHandler())
                    .end()

                    //Step 7: Process the EHR Input
                    .doTry()
                        //Get the mapped openEHRId if avaialbe else create new ehrId
                        .to("direct:patientIdToEhrIdMapperProcess")
                        .log("Patient ID mapped to EHR ID: ${header.CamelEhrCompositionEhrId}")
    
                        // .wireTap("direct:OpenEHRProcess")
                        .to("direct:OpenEHRProcess")
                    .doCatch(Exception.class)
                        .log("direct:OpenEHRProcess catch exception")
                        .process(new OpenEhrClientExceptionHandler())
                    .end()

                    //Step 8: Prepare the final output
                    .process(ProvideResourceResponseProcessor.BEAN_ID)
                .end();

        }
}