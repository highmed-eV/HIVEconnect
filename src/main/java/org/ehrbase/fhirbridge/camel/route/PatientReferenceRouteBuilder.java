package org.ehrbase.fhirbridge.camel.route;

import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.fhir.camel.CompositionLookupProcessor;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.springframework.stereotype.Component;


@Component
public class PatientReferenceRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route to perform the ETL for the FHIR request
        from("direct:PatientReferenceProcessor")
        //Perform ETL
            .doTry()
                .to("direct:ExtractProcess")
                .to("direct:ValidateAndEnrichProcess")
            .doCatch(Exception.class)
                .log("direct:PatientReferenceProcessor exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end();


        //Extract
        from("direct:ExtractProcess")
            .doTry()
                .process(exchange -> {
                    if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                            FhirUtils.extractInputParameters(exchange);
                        }
                    })
            .doCatch(Exception.class)
                .log("direct:handleCreateOperationETL exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end();
            

        //Validate and Enrich
        from("direct:ValidateAndEnrichProcess")
            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} != 'Patient'"))
                    .doTry()
                        // Step 1: Extract Patient Id from the FHIR Input Resource
                        .to("direct:extractAndValidatePatientIdExistsProcessor")
                        .log("FHIR Patient ID: ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                        // Step 2: Validate: 
                        //Check the incoming profile is supported by openFHIR
                        .to("direct:validateOpenFHIRProfilesProcess")
                    .doCatch(Exception.class)
                        .log("direct:ValidateAndEnrichProcess exception")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
            .otherwise()
                .doTry()
                    .to("direct:extractPatientIdFromPatientProcessor")
                    .log("FHIR Patient ID:  ${header." + CamelConstants.PATIENT_ID + "}")
                .doCatch(Exception.class)
                    .log("direct:extractPatientIdFromPatientProcessor exception")
                    .process(new FhirBridgeExceptionHandler())
                .endDoTry()
                .endChoice()
            .end()

            //Check if the input resource or input bundle resource consisting of same resources already exist
            // If already exist : throw duplicate resource or bundle resource creation
            .doTry()
                .to("direct:checkDuplicateResource")
            .doCatch(Exception.class)
                .log("direct:checkDuplicateResource exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()

            // Step 3:  Enrich: Extract and update the input reference Resources from the input fhir bundle
            .doTry()
                .to("direct:mapReferencedInternalResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:mapReferencedInternalResourceProcessor exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end();

        //Validate and Enrich
        from("direct:checkDuplicateResource")
            .routeId("CheckDuplicateResourceRoute")

            // 1. Retrieve the list of all input resource ids
            .process(exchange -> {
                String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                List<String> inputResourceIds = FhirUtils.getInputResourceIds(inputResource);
                exchange.setProperty(CamelConstants.INPUT_RESOURCE_IDS, inputResourceIds);
            })
            .log("FHIR Input Resource ID(s) : ${header." + CamelConstants.INPUT_RESOURCE_IDS + "}")
            // 2. Check the mapping table : FB_RESOURCE_COMPOSITION
            // and get the compositionId(s) for corresponding inputResourceId(s).
            // 3. If all compositionId(s) is/are same and operation is POST
            //     throw duplicate bundle resource exception.
            .choice()
                .when(simple("${exchangeProperty." + CamelConstants.INPUT_RESOURCE_IDS + "} != null && ${exchangeProperty." + CamelConstants.INPUT_RESOURCE_IDS + ".size()} > 0"))

                    .doTry()
                        .process(CompositionLookupProcessor.BEAN_ID)
                    .doCatch(Exception.class)
                        .log("CheckDuplicateResource: CompositionLookupProcessor exception")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
            .endChoice()
            .log("CheckDuplicateResource: No duplicate resources found in the input resource or input bundle");

        }
}