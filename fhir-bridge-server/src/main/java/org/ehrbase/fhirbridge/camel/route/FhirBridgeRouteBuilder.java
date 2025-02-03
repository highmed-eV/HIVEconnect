package org.ehrbase.fhirbridge.camel.route;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.exception.OpenEhrClientExceptionHandler;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class FhirBridgeRouteBuilder extends RouteBuilder {

//    private PatientIdMapper patientIdMapper;
//    private final Authenticator authenticator;
//
//    public FhirBridgeRouteBuilder(PatientIdMapper patientIdMapper,
//                        Authenticator authenticator) {
//        this.patientIdMapper = patientIdMapper;
//        this.authenticator = authenticator;
//    }

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)
            .log("FhirBridgeRouteBuilder Exception caught: ${exception.class} - ${exception.message}")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
            .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
            .process(exchange -> {
                Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                if (exception instanceof BaseServerResponseException baseException) {
                    if (baseException.getOperationOutcome() != null) {
                        // Set body with serialized operation outcome if present
                        exchange.getIn().setBody(FhirUtils.serializeOperationOutcome(baseException.getOperationOutcome()));
                    } else {
                        // If operation outcome is not present, set the exception message
                        exchange.getIn().setBody(baseException.getMessage());
                    }
                } else {
                    // If the exception is not of type BaseServerResponseException
                    exchange.getIn().setBody(exception.getMessage());
                }
            })
            .log("######### FhirBridgeRouteBuilder onException");


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
            // .marshal().json()
            // .convertBodyTo(String.class)
            .log("##########RFHIRIBridgeProcess")
            .process(exchange -> {
                    if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                        String inputResource = (String) exchange.getIn().getBody();
                        String inputResourceType = FhirUtils.getResourceType(inputResource);
                        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, inputResource);
                        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE_TYPE, inputResourceType);
                    }
                })
            .log("FHIR Resource Type ${header.CamelFhirBridgeIncomingResourceType }")
            // .to("direct:ExtractPatientIdProcess")
            // .to("direct:FHIRToOpenEHRMappingProcess")
            .log("FHIRBridgeProcess body: ${body}");
            


        from("direct:ExtractPatientIdProcess")
            // Step 1: Extract Patient Id from the FHIR Input Resource
            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} != 'Patient'"))
                    .to("direct:extractAndCheckPatientIdExistsProcessor")
                    .log("FHIR Patient ID  ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                    .endChoice()
                .otherwise()
                    .to("direct:extractPatientIdFromPatientProcessor")
                    .log("FHIR Patient ID  ${header." + CamelConstants.PATIENT_ID + "}")
                .endChoice();

        from("direct:FHIRToOpenEHRMappingProcess")

            // Step 2: Check if the input resource or input bundle resource consisting of same resources already exist
            // If already exist : throw duplicate resource or bundle resource creation
            .doTry()
                .to("direct:checkDuplicateResource")
            .doCatch(Exception.class)
                .log("direct:checkDuplicateResource catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()


            // Step 3:  Extract and update the input reference Resources from the input fhir bundle
            .doTry()
                .to("direct:mapInternalResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:mapInternalResourceProcessor catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()


            .doTry()
                // Step 4: Forward request to FHIR server
                .to("direct:FHIRProcess")
                // Step 5: Extract Patient Id created in the FHIR server
                .to("direct:extractPatientIdFromFhirResponseProcessor")
            .doCatch(Exception.class)
                .log("direct:FHIRProcess catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()
            // marshall to JSON for logging
            // .marshal().fhirJson("{{fhirVersion}}")
            // .log("Inserting Patient: ${body}")

            // Step 6: Add the extracted reference Resources to the input fhir bundle
            .doTry()
                .to("direct:resourceReferenceProcessor")
            .doCatch(Exception.class)
                .log("direct:resourceReferenceProcessor catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()

            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} == 'Patient'"))
                    //Get the mapped openEHRId if available else create new ehrId
                    .to("direct:patientIdToEhrIdMapperProcess")
                    .log("Patient ID mapped to EHR ID: ${header.CamelEhrCompositionEhrId}")
                    // Prepare the final output 
                    .process(ProvideResourceResponseProcessor.BEAN_ID)
                .otherwise()
                    //Step 7: Process the openFHIR Input
                    .doTry()
                        .to("direct:OpenFHIRProcess")
                    .doCatch(Exception.class)
                        .log("direct:OpenFHIRProcess catch exception")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                    .end()

                    //Step 8: Process the EHR Input
                    .doTry()
                        //Get the mapped openEHRId if avaialbe else create new ehrId
                        .to("direct:patientIdToEhrIdMapperProcess")
                        .log("Patient ID mapped to EHR ID: ${header.CamelEhrCompositionEhrId}")
    
                        // .wireTap("direct:OpenEHRProcess")
                        .to("direct:OpenEHRProcess")
                    .doCatch(ClientException.class)
                        .log("direct:OpenEHRProcess catch exception")
                        .process(new OpenEhrClientExceptionHandler())
                    .endDoTry()
                    .end()

                    //Step 9: Prepare the final output
                    .process(ProvideResourceResponseProcessor.BEAN_ID)
                .end();

        }
}