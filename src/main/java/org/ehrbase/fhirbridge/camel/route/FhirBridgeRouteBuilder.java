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

        // Route to perform the ETL for the FHIR request
        from("direct:FHIRBridgeETLProcess")
        //Perform ETL
            .to("direct:ExtractProcess")
            .to("direct:ValidateAndEnrichProcess")
            //Skip Transform and load in case of Patient resource
            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} == 'Patient'"))
                    .doTry()
                        //Get the mapped openEHRId if available else create new ehrId
                        .to("direct:patientIdToEhrIdMapperProcess")
                    .doCatch(Exception.class)
                        .log("direct:FHIRBridgeETLProcess catch exception")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
            .otherwise()
                .to("direct:TransformProcess")
                .to("direct:LoadProcess")
            .end()
            // Prepare the final output 
            .to("direct:OutputProcess")
            .log("FHIRBridgeETLProcess body: ${body}");

        //Extract
        from("direct:ExtractProcess")
            .doTry()
                .process(exchange -> {
                    if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                            FhirUtils.extractInputParameters(exchange);
                        }
                    })
            .doCatch(Exception.class)
                .log("direct:FHIRBridgeETLProcess catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()
            .log("FHIR Resource Type ${header.CamelFhirBridgeIncomingResourceType }");
            

        //Validate and Enrich
        from("direct:ValidateAndEnrichProcess")
            .choice()
                .when(simple("${header.CamelFhirBridgeIncomingResourceType} != 'Patient'"))
                    .doTry()
                        // Step 1: Extract Patient Id from the FHIR Input Resource
                        .to("direct:extractAndValidatePatientIdExistsProcessor")
                        .log("FHIR Patient ID  ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                        // Step 2: Validate: 
                        //Check the incoming profile is supported by openFHIR
                        .to("direct:validateOpenFHIRProfilesProcess")
                    .doCatch(Exception.class)
                        .log("direct:ValidateAndEnrichProcess catch exception")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
            .otherwise()
                .to("direct:extractPatientIdFromPatientProcessor")
                .log("FHIR Patient ID  ${header." + CamelConstants.PATIENT_ID + "}")
            .end()

            //Check if the input resource or input bundle resource consisting of same resources already exist
            // If already exist : throw duplicate resource or bundle resource creation
            .doTry()
                .to("direct:checkDuplicateResource")
            .doCatch(Exception.class)
                .log("direct:checkDuplicateResource catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()

            // Step 3:  Enrich: Extract and update the input reference Resources from the input fhir bundle
            .doTry()
                .to("direct:mapReferencedInternalResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:mapReferencedInternalResourceProcessor catch exception")
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
                .to("direct:referencedResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:referencedResourceProcessor catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end();

        //Transform
        from("direct:TransformProcess")
            //Step 7: Process the openFHIR Input
            .doTry()
                .to("direct:OpenFHIRProcess")
            .doCatch(Exception.class)
                .log("direct:OpenFHIRProcess catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end();

        //Load
        from("direct:LoadProcess")
            //Step 8: Process the EHR Input
            .doTry()
                //Get the mapped openEHRId if avaialbe else create new ehrId
                .to("direct:patientIdToEhrIdMapperProcess")
                // .wireTap("direct:OpenEHRProcess")
                .to("direct:OpenEHRProcess")
            .doCatch(ClientException.class)
                .log("direct:OpenEHRProcess catch exception")
                .process(new OpenEhrClientExceptionHandler())
            .endDoTry()
            .end();

        from("direct:OutputProcess")
            //Step 9: Prepare the final output
            .process(ProvideResourceResponseProcessor.BEAN_ID);
        }
}