package org.ehrbase.fhirbridge.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@Component
@SuppressWarnings("java:S1192")
public class FhirRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        from("direct:FHIRProcess")
            // Forward request to FHIR server
            .choice()
                .when().jsonpath("$[?(@.type == 'transaction')]")
                    // if body.type == "transaction"
                    // create Transaction bundle in our FHIR server
                    .log("Transaction FHIR request. Starting process...")
                    .to("fhir://transaction/withBundle?inBody=stringBundle&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
            .otherwise()
                // else create Resource in our FHIR server
                .log("Resource FHIR request. Starting process...")
                .to("fhir://create/resource?inBody=resourceAsString&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
            .end()
            .log("FHIR request processed by FHIR server ${body}")

            //Store the response in the Exchange
            .process(exchange -> {
                //Response may not be resource. It is OutCome
                Resource response = exchange.getIn().getBody(Resource.class);
                exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, response);
            });


        // Extract Patient Id from the FHIR Input Resource
        from("direct:patientReferenceProcessor")
            .routeId("PatientReferenceProcessorRoute")
            
            // Extract or find the Patient ID from the resource
            .process(exchange -> {
                String  resource = exchange.getIn().getBody(String.class);
                String patientId = FhirUtils.getPatientId(resource);
                exchange.setProperty(CamelConstants.PATIENT_ID, patientId);
            })
            .log("FHIR Patient ID ${exchangeProperty.CamelFhirPatientId}")


            // Cannot throw ResourceNotFound here. As the patient can be created 
            // Internal Reference : verify the id is in the server: update the mapper table
            // Internal contained reference : get the sever id after the respource is created and then update themapper table
            // Transaction reference : get the sever id after the respource is created and then update themapper table
            // External reference (absolute URL) : add to the mapper table as is
                
            // If patient is found in server (PatientResourceFromServer) store this patientId 
            // as serverPatientId in excahnge so that it can be added to fhi-patient-id to ehr-id mapp table
            //else it has to be created after the resource is created in the server.
            .choice()
                .when(simple("${exchangeProperty.CamelFhirPatientId} != null"))
                    .doTry()
                        .toD("fhir://read/resourceById?resourceClass=Patient&stringId=${exchangeProperty.CamelFhirPatientId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .process(exchange -> {
                            if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                                Patient patientResource = (Patient) exchange.getIn().getBody();
                                String serverPatientId = patientResource.getId();
                                exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_RESOURCE, patientResource);
                                exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                            }
                        })
                    .doCatch(ResourceNotFoundException.class)
                        .throwException(ResourceNotFoundException.class, "${exception.message}")
                    .end()
                .endChoice()
            .otherwise()
                .throwException(ResourceNotFoundException.class, "PatientId does not exist")
            .end()

            .log("FHIR Patient ID exists")
            .process(exchange -> {
                //set back the input json as body
                String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                exchange.getIn().setBody(inputResource);
            })
            .log("FHIR PatientReferenceProcessor completed: input patient id: ${exchangeProperty.CamelFhirServerPatientId} and server patient id: ${exchangeProperty.CamelFhirServerPatientId}");
        
        // Extract Patient Id from the FHIR Input Resource
        from("direct:patientReferencePostProcessor")
            .routeId("patientReferencePostProcessorRoute")
            // (From FhirUtils)
            // if Internal contained reference
            // or Transaction reference
            // Extract or find the Patient ID from the resource
            .process(exchange -> {
                //May not be resource. It will be OutCome
                Resource  resource = (Resource) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
                String serverPatientId = FhirUtils.getPatientIdFromOutCome(resource);
                exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                //Need to set SERVER_PATIENT_RESOURCE
            })
            .log("FHIR server Patient ID ${exchangeProperty.CamelFhirServerPatientId}");
    }    

    // @Override
    // public void configure() throws Exception {
        
    //     // Entry point: Receive a FHIR resource or bundle
    //     from("direct:patientIdExtractor")
    //         .routeId("PatientIdMapperProcessRoute")
            
    //         // Step 1: Extract or find the Patient ID from the resource
    //         .process(exchange -> {
    //             String  resource = exchange.getIn().getBody(String.class);
    //             String patientId = FhirUtils.getPatientId(resource);
    //             exchange.setProperty("FHIRPatientId", patientId);
    //         })
    //         .log("FHIR Patient ID ${exchangeProperty.FHIRPatientId}")


    //         // Step 2: Check if the patient exists in the FHIR server
    //         .choice()
    //             .when(simple("${exchangeProperty.FHIRPatientId} != null"))
    //                 .doTry()
    //                     .toD("fhir://read/resourceById?resourceClass=Patient&stringId=${exchangeProperty.FHIRPatientId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
    //                 .doCatch(ResourceNotFoundException.class)
    //                     .process(exchange -> {
    //                         exchange.setProperty("FHIRPatientExists", false);
    //                     })
    //                 .end()
    //             .endChoice()
    //         .otherwise()
    //             .setProperty("FHIRPatientExists", constant(false))
    //         .end()

    //         .log("FHIR Patient ID exists ${exchangeProperty.FHIRPatientExists}")
    //         // Step 3: If patient does not exist, create a new Patient
    //         .choice()
    //             .when(simple("${exchangeProperty.FHIRPatientExists} == false"))
    //                 .process(exchange -> {
    //                     // Construct a new Patient resource as needed
    //                     Patient newPatient = new Patient();
    //                     newPatient.setId(exchange.getProperty("FHIRPatientId", String.class));
    //                     exchange.getIn().setBody(newPatient);
    //                 })
    //                 .to("fhir://create/resource?inBody=resource&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
    //                 //TODO : add doTry and doCatch and handle exception
    //                 // .process(exchange -> {
    //                 //     exchange.setProperty("FHIRPatientId", createdPatient.getIdElement().getIdPart());
    //                 // })
    //         .end()
    //         .process(exchange -> {
    //             //set back the input json as body
    //             String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
    //             exchange.getIn().setBody(inputResource);
    //         })
    //         .log("FHIR PatientId Mapper Process Route completed: ${exchangeProperty.FHIRPatientId}");
    // }
}

