package org.highmed.hiveconnect.camel.route;

import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.highmed.hiveconnect.exception.HiveConnectExceptionHandler;
import org.highmed.hiveconnect.fhir.camel.CompositionLookupProcessor;
import org.highmed.hiveconnect.fhir.support.FhirUtils;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;


@Component
public class PatientReferenceRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route to perform the ETL for the FHIR request
        from("direct:PatientReferenceProcessor")
        //Perform Extract and Enrich
            .doTry()
                // Extract Request type(Bundle or Resource),
                // method, source to be added in openehr context
                // and profiles to be validated if it is supported by openFHIR for transformation
                .to("direct:ExtractProcess")
                
                // Validate 
                // 1. Extract  for all flavours of patientId(relative, identifier, absolute
                // and validate if it exists in the fhir server
                // and get the server patient id from db if present

                //2. Check the incoming profile is supported by openFHIR

                //3. Check if the input resource or input bundle resource consisting of same resources already exist
                // If already exist : throw duplicate resource or bundle resource creation

                //4. Extract Reference(all  flavours: local, relative, absolute, or internal) Resource Ids 
                // from the FHIR Input Resource, lookup db and maintain the mapping

                .to("direct:ValidatePatientAndResourceReferenceProcess")
            .doCatch(Exception.class)
                .log("direct:PatientReferenceProcessor exception")
                .process(new HiveConnectExceptionHandler())
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
                .log("direct:ExtractProcess exception")
                .process(new HiveConnectExceptionHandler())
            .endDoTry()
            .end();
            

        //Validate and Enrich
        from("direct:ValidatePatientAndResourceReferenceProcess")
            .choice()
                .when(simple("${header.CamelRequestResourceResourceType} != 'Patient'"))
                    .doTry()
                        // Extract  for all flavours of patientId(relative, identifier, absolute
                        // and validate if it exists in the fhir server
                        // and get the server patient id from db if present
                        .to("direct:extractAndValidatePatientIdExistsProcessor")
                        .log("FHIR ServerPatient ID: ${header." + CamelConstants.FHIR_SERVER_PATIENT_ID + "}")
                        
                        // Validate: 
                        //Check the incoming profile is supported by openFHIR
                        .to("direct:validateOpenFHIRProfilesProcess")
                    .doCatch(Exception.class)
                        .log("direct:ValidatePatientAndResourceReferenceProcess exception")
                        .process(new HiveConnectExceptionHandler())
                    .endDoTry()
                .endChoice()
            .otherwise()
                .doTry()
                    // Extract Patient Id from the Patient resource
                    // and get the server patient id from db if present
                    // and validate if it exists in the fhir server
                    // Not validating the profile as we are not transforming to an openehr template
                    .to("direct:extractPatientIdFromPatientProcessor")
                    .log("FHIR Patient ID:  ${header." + CamelConstants.FHIR_INPUT_PATIENT_ID + "}")
                .doCatch(Exception.class)
                    .log("direct:extractPatientIdFromPatientProcessor exception")
                    .process(new HiveConnectExceptionHandler())
                .endDoTry()
                .endChoice()
            .end()

            //Check if the input resource or input bundle resource consisting of same resources already exist
            // If already exist : throw duplicate resource or bundle resource creation
            .doTry()
                .to("direct:checkDuplicateResource")
            .doCatch(Exception.class)
                .log("direct:checkDuplicateResource exception")
                .process(new HiveConnectExceptionHandler())
            .endDoTry()
            .end()

            /// Extract Reference(all  flavours: local, relative, absolute, or internal) Resource Ids 
            // from the FHIR Input Resource, lookup db and maintain the mapping
            .doTry()
                .to("direct:mapReferencedInternalResourceProcessor")
            .doCatch(Exception.class)
                .log("direct:mapReferencedInternalResourceProcessor exception")
                .process(new HiveConnectExceptionHandler())
            .endDoTry()
            .end();

        //Validate and Enrich
        from("direct:checkDuplicateResource")
            .routeId("CheckDuplicateResourceRoute")

            // 1. Retrieve the list of all input resource ids
            .process(exchange -> {
                Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
                List<String> inputResourceIds = FhirUtils.getInputResourceIds(inputResource);
                exchange.setProperty(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, inputResourceIds);
            })
            .log("FHIR Input Resource ID(s) : ${header." + CamelConstants.FHIR_REQUEST_RESOURCE_IDS + "}")
            // 2. Check the mapping table : FB_RESOURCE_COMPOSITION
            // and get the compositionId(s) for corresponding inputResourceId(s).
            // 3. If all compositionId(s) is/are same and operation is POST
            //     throw duplicate bundle resource exception.
            .choice()
                .when(simple("${exchangeProperty." + CamelConstants.FHIR_REQUEST_RESOURCE_IDS + "} != null && ${exchangeProperty." + CamelConstants.FHIR_REQUEST_RESOURCE_IDS + ".size()} > 0"))

                    .doTry()
                        .process(CompositionLookupProcessor.BEAN_ID)
                    .doCatch(Exception.class)
                        .log("CheckDuplicateResource: CompositionLookupProcessor exception")
                        .process(new HiveConnectExceptionHandler())
                    .endDoTry()
            .endChoice()
            .log("CheckDuplicateResource: No duplicate resources found in the input resource or input bundle");

        }
}