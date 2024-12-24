package org.ehrbase.fhirbridge.camel.route;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.fhir.camel.ExistingResourceReferenceProcessor;
import org.ehrbase.fhirbridge.fhir.camel.ResourceLookupProcessor;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

                                 //set back the input json as body
                                String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                                exchange.getIn().setBody(inputResource);
                                                
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

                            //set back the input json as body
                            String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                            exchange.getIn().setBody(inputResource);
                                        
                        })
                    .doCatch(Exception.class)
                        .log("direct:FHIRProcess fhir://create catch exception")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .end()
                .log("FHIR request processed by FHIR server ${body}");

        // Extract Patient Id from the FHIR Input Resource
        from("direct:extractAndCheckPatientIdExistsProcessor")
            .routeId("ExtractAndheckPatientIdExistsProcessorRoute")
            //Get the patientid from input resource(Bundle, Patient or any resource)
            //find the patient id in the fhir server
            
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
        
        // Extract Patient Id from the FHIR Server response
        from("direct:extractPatientIdFromFhirResponseProcessor")
            .routeId("extractPatientIdFromFhirResponseProcessorRoute")
            // (From FhirUtils)
            // if Internal contained reference
            // or Transaction reference
            // Extract or find the Patient ID from the resource
            .choice()
                .when(header(CamelConstants.SERVER_PATIENT_ID).isNull())
                    .process(exchange -> {
                        //May not be resource. It will be OutCome
                        MethodOutcome resource = (MethodOutcome) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
                        String serverPatientId = FhirUtils.getPatientIdFromOutCome(resource);
                        exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                        exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_RESOURCE, (Patient)resource.getResource());
                    })
                    .log("FHIR server Patient ID: ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                .otherwise()
                    .log("FHIR server Patient ID already set: ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
            .end();

        // Extract Reference Resource Ids from the FHIR Input Resource
        from("direct:mapInternalResourceProcessor")
            .routeId("MapInternalResourceProcessor")

            // 1. Extract or find the Reference Resource ID(s) from the resource
            // 2. Check if the reference resource Id(s) already exist in the bundle or not.
            // If the reference resource Id(s) doesn't exist in the bundle.
            // Add it in the referenceResourceIds(excluding subject.reference)
            .process(exchange -> {
                // String resource = exchange.getIn().getBody(String.class);
                String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                List<String> referenceInputResourceIds = FhirUtils.getResourceIds(inputResource);
                exchange.setProperty(CamelConstants.REFERENCE_INPUT_RESOURCE_IDS, referenceInputResourceIds);
            })
            .log("FHIR Reference Resource ID(s) : ${exchangeProperty." + CamelConstants.REFERENCE_INPUT_RESOURCE_IDS + "}")
            // 3. Check the mapping table : FB_RESOURCE_COMPOSITION
            // and get the internalResourceId(s) for corresponding inputResourceId(s).
            // 4. Replace the reference inputResourceId(s) with the internalResourceId(s) in the input fhir bundle
            .choice()
                .when(exchangeProperty(CamelConstants.REFERENCE_INPUT_RESOURCE_IDS).isNotNull())
                    .log("Reference Resource IDs: " + CamelConstants.REFERENCE_INPUT_RESOURCE_IDS)
                    .process(ResourceLookupProcessor.BEAN_ID)
            .end()
            .log("FHIR INTERNAL RESOURCE ID(s) : ${exchangeProperty." + CamelConstants.INTERNAL_RESOURCE_IDS + "}")
            .log("Updated input resouce bundle with the internalResourceIds");

        // Add the extracted Reference Resource Ids as resource in the FHIR Input Bundle Resource
        from("direct:resourceReferenceProcessor")
            .routeId("ResourceReferenceProcessorRoute")
            .choice()
                .when(exchangeProperty(CamelConstants.INTERNAL_RESOURCE_IDS).isNotNull())
                // .when(simple("${exchangeProperty.CamelConstants.INTERNAL_RESOURCE_IDS} != null"))
                    // 1. Fetch the resources for the internalResourceId(s) is/are in the server.
                    // 2. Add the resources in the input fhir bundle.
                    .log("Property" + CamelConstants.INTERNAL_RESOURCE_IDS + "is present.")
                    // Process the list of internal resource IDs
                    .process(exchange -> {
                        // Retrieve the list of resource strings from the property
                        List<String> resourceIds = exchange.getProperty(CamelConstants.INTERNAL_RESOURCE_IDS, List.class);
                        // Add the list to the exchange body for splitting
                        exchange.getIn().setBody(resourceIds);
                        // Initialize the list before split
                        List<String> existingResources = exchange.getProperty(CamelConstants.SERVER_EXISTING_RESOURCES, List.class);
                        if (existingResources == null) {
                            existingResources = new ArrayList<>();
                            exchange.setProperty(CamelConstants.SERVER_EXISTING_RESOURCES, existingResources);
                        }
                    })
                    // Split the list to process each resource string individually
                    .split(body()).shareUnitOfWork()
                        .process(exchange -> {
                            // Extract resourceClass and stringId from the current string
                            String resourceString = exchange.getIn().getBody(String.class);
                            // Use regular expression to extract resourceType and id
                            Pattern pattern = Pattern.compile("([^/]+)/([^/]+)");
                            Matcher matcher = pattern.matcher(resourceString);
                            if (matcher.matches()) {
                                exchange.setProperty(CamelConstants.INTERNAL_RESOURCE_TYPE, matcher.group(1));
                                exchange.setProperty(CamelConstants.STRING_INTERAL_ID, matcher.group(2));
                            }
                        })
                        .log("Processing resource: Class = ${exchangeProperty.FhirServerResourceType}, ID = ${exchangeProperty.FhirServerExistingId}")
                        .doTry()
                            .toD("fhir:read/resourceById?resourceClass=${exchangeProperty.FhirServerResourceType}&stringId=${exchangeProperty.FhirServerExistingId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                            .process(exchange -> {
                                if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                                    // Retrieve existing resources or initialize list
                                    List<String> existingResources = exchange.getProperty(CamelConstants.SERVER_EXISTING_RESOURCES, List.class);
                                    // Add resource response to the list
                                    Resource resourceResponse = exchange.getIn().getBody(Resource.class);
                                    // Convert the resource to String using HAPI FHIR JSON parser
                                    FhirContext fhirContext = FhirContext.forR4();
                                    String resourceResponseStr = fhirContext.newJsonParser().encodeResourceToString(resourceResponse);
                                    existingResources.add(resourceResponseStr);
                                    exchange.setProperty(CamelConstants.SERVER_EXISTING_RESOURCES, existingResources);
                                }
                            })
                        .doCatch(ResourceNotFoundException.class)
                            .log("Resource not found for resourceClass=${exchangeProperty.resourceClass}, stringId=${exchangeProperty.stringId}. Skipping...")
                    .end() // End the split block
            .end()
            .process(ExistingResourceReferenceProcessor.BEAN_ID)
            .log("Updated input resouce bundle with the referece resources");
    }

}

