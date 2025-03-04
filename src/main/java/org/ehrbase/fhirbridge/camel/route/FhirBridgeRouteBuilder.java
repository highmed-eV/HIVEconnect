package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.exception.OpenEhrClientExceptionHandler;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.springframework.stereotype.Component;


@Component
public class FhirBridgeRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route to process the FHIR request
        from("direct:FHIRBridgeProcess")
            .doTry()
                .process(exchange -> {
                        if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                            String inputResource = (String) exchange.getIn().getBody();
                            String inputResourceType = FhirUtils.getResourceType(inputResource);
                            exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, inputResource);
                            exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE_TYPE, inputResourceType);
                            //extract and verify the request is either POST or PUT
                            //In case of Bundle all the  resources should have the same request http method
                            FhirUtils.extractInputMethod(exchange);
                        }
                    })
            .doCatch(Exception.class)
                .log("direct:FHIRBridgeProcess catch exception")
                .process(new FhirBridgeExceptionHandler())
            .end()
            .log("FHIR Resource Type ${header.CamelFhirBridgeIncomingResourceType }")
            .to("direct:ExtractPatientIdProcess")
            .to("direct:FHIRToOpenEHRMappingProcess")
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