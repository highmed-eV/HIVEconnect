package org.ehrbase.fhirbridge.camel.route;

import ca.uhn.fhir.rest.api.MethodOutcome;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@Component
@SuppressWarnings("java:S1192")
public class FhirRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        onException(BaseServerResponseException.class)
        .handled(true)      
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
        .setBody(exceptionMessage())
        // .useOriginalBody()
        .log("######### FhirRouteBuilder")
        ;

        // onException(ResourceNotFoundException.class)
        // .handled(true)      
        // .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        // .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
        // .setBody(exceptionMessage())
        // // .useOriginalBody()
        // .log("######### FhirRouteBuilder")
        // ;

        from("direct:FHIRProcess")
            // Forward request to FHIR server
                .choice()
                    .when().jsonpath("$[?(@.type == 'transaction')]")
                        // if body.type == "transaction"
                        // create Transaction bundle in our FHIR server
                        .log("Transaction FHIR request. Starting process...")
                        .doTry()
                            .to("fhir://transaction/withBundle?inBody=stringBundle&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                            //Store the response in the Exchange
                            .process(exchange -> {
                                //Response may not be resource. It is OutCome
                                String response = exchange.getIn().getBody(String.class);
                                exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, response);
                            })
                        .doCatch(Exception.class)
                            .log("direct:FHIRProcess fhir://transaction catch exception")
                            .process(new FhirBridgeExceptionHandler())
                        .endDoTry()

                    .endChoice()
                .otherwise()
                    // else create Resource in our FHIR server
                    .log("Resource FHIR request. Starting process...")
                    .doTry()
                        .to("fhir://create/resource?inBody=resourceAsString&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        //Store the response in the Exchange
                        .process(exchange -> {
                            //Response may not be resource. It is OutCome
                            MethodOutcome response = exchange.getIn().getBody(MethodOutcome.class);
                            exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, response);
                        })
                    .doCatch(Exception.class)
                        .log("direct:FHIRProcess fhir://create catch exception")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .end()
                .log("FHIR request processed by FHIR server ${body}");

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
                    .endDoTry()
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
            .choice()
                .when(header(CamelConstants.SERVER_PATIENT_ID).isNull())
                    .process(exchange -> {
                        //May not be resource. It will be OutCome
                        String resource = (String) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
                        String serverPatientId = FhirUtils.getPatientIdFromOutCome(resource);
                        exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                        //Need to set SERVER_PATIENT_RESOURCE
                    })
                    .log("FHIR server Patient ID: ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                .otherwise()
                    .log("FHIR server Patient ID already set: ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
            .end();
    }    

}

