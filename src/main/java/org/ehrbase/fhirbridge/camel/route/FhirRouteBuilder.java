package org.ehrbase.fhirbridge.camel.route;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.fhir.camel.*;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.ehrbase.fhirbridge.fhir.support.PatientUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@SuppressWarnings("java:S1192")
public class FhirRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // onException(BaseServerResponseException.class)
        //     .handled(true)
        //     .log("FhirRouteBuilder Exception caught: ${exception.class} - ${exception.message}")
        //     .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
        //     .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
        //     .process(exchange -> {
        //         BaseServerResponseException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, BaseServerResponseException.class);
        //         if (exception != null && exception.getOperationOutcome() != null) {
        //             // String serializedOutcome = FhirUtils.serializeOperationOutcome(exception.getOperationOutcome());
        //             MethodOutcome methodOutcome = new MethodOutcome();
        //             methodOutcome.setCreated(true);
        //             methodOutcome.setOperationOutcome(exception.getOperationOutcome());
        //             exchange.getIn().setBody(methodOutcome);
        //         } else if (exception != null) {
        //             exchange.getIn().setBody(exception.getMessage());
        //         }
        //     })
        //     .log("######### FhirRouteBuilder onException");

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
        from("direct:extractPatientIdFromPatientProcessor")
            .routeId("extractPatientIdFromPatientProcessorRoute")
            //Get the patientid from input resource(Bundle, Patient or any resource)
            //find the patient id in the fhir server
            .doTry()
                // Extract or find the Patient ID from the resource and get the server patient id from db
                .bean(PatientUtils.class, "getPatientIdFromPatientResource")
            .doCatch(Exception.class)
                .log("direct:extractPatientIdFromPatientProcessor exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .log("FHIR PatientId ${header." + CamelConstants.PATIENT_ID + "}" );

        // Extract Patient Id from the FHIR Input Resource
        from("direct:extractAndCheckPatientIdExistsProcessor")
            .routeId("extractAndCheckPatientIdExistsProcessorRoute")
            //Get the patientid from input resource(Bundle, Patient or any resource)
            //find the patient id in the fhir server
            
            // Extract or find the Patient ID from the resource and get the server patient id from db
            .doTry()
                .bean(PatientUtils.class, "extractPatientIdOrIdentifier")
            .doCatch(Exception.class)
                .log("extractPatientIdOrIdentifier catch exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()

            .log("FHIR PatientId ${header." + CamelConstants.PATIENT_ID 
                    + "}, Server PatientId ${header." 
                    + CamelConstants.SERVER_PATIENT_ID 
                    + "}, PatientId Type ${header."
                    + CamelConstants.PATIENT_ID_TYPE
                    + "}")

            .choice()
            .when(header(CamelConstants.SERVER_PATIENT_ID).isNull())
                .to("direct:checkPatientIdExistsProcessor");
            
        from ("direct:checkPatientIdExistsProcessor")
            .routeId("checkPatientIdExistsProcessorRoute")
            
            // Cannot throw ResourceNotFound here. As the patient can be created 
            // Internal Reference : verify the id is in the server: update the mapper table
            // Internal contained reference : get the sever id after the respource is created and then update themapper table
            // Transaction reference : get the sever id after the respource is created and then update themapper table
            // External reference (absolute URL) : add to the mapper table as is
                
            // If patient is found in server (PatientResourceFromServer) store this patientId 
            // as serverPatientId in excahnge so that it can be added to fhi-patient-id to ehr-id mapp table
            //else it has to be created after the resource is created in the server.
            .choice()
                .when(header(CamelConstants.PATIENT_ID_TYPE).isEqualTo("RELATIVE_REFERENCE"))
                    .doTry()
                        .log("Read RELATIVE_REFERENCE patient id ${header.CamelFhirPatientId}")
                        .toD("fhir://read/resourceById?resourceClass=Patient&stringId=${header.CamelFhirPatientId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .process(ExistingServerPatientResourceProcessor.BEAN_ID)
                    .doCatch(Exception.class)
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
                //else if identifier
                .when(header(CamelConstants.PATIENT_ID_TYPE).isEqualTo("IDENTIFIER"))
                    .doTry()
                        .log("Search IDENTIFIER patient id for identifier ${header." + CamelConstants.IDENTIFIER_STRING + "}")
                        .toD("fhir://search/searchByUrl?url=Patient?identifier=${header.CamelIdentifierString}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .log("Response Search patient id  for identifier ${body}")
                        .process(exchange -> {
                            if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
                                Bundle patientBundleResource = (Bundle) exchange.getIn().getBody();
                                Patient serverPatient = Optional.ofNullable(patientBundleResource.getEntry())
                                                        .filter(entryList -> !entryList.isEmpty())
                                                        .map(entryList -> entryList.get(0).getResource())
                                                        .filter(Patient.class::isInstance)
                                                        .map(Patient.class::cast)
                                                        .orElse(null);

                                if( serverPatient == null) {
                                    TokenParam  tokenParam = (TokenParam) exchange.getProperty(CamelConstants.IDENTIFIER_OBJECT);
                                    Identifier identifier=  new Patient().addIdentifier();
                                    identifier.setValue(tokenParam.getValue());
                                    identifier.setSystem(tokenParam.getSystem());
                                    Patient patient = new Patient().addIdentifier(identifier);
                                    exchange.getIn().setBody(patient);
                                } else {
                                    String serverPatientId = "Patient/" + serverPatient.getIdPart();
                                    exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_RESOURCE, serverPatient);
                                    exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                                    exchange.getIn().setBody(serverPatient);
                                }
                            }
                        })
                    .log("Response Search body  ${body}")

                    .log("Search server IDENTIFIER patient id ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                    .doCatch(ResourceNotFoundException.class)
                        .log("Patient Identifier Not Found")
                        .process(exchange -> { 
                            TokenParam  tokenParam = (TokenParam) exchange.getProperty(CamelConstants.IDENTIFIER_OBJECT);
                            Identifier identifier=  new Patient().addIdentifier();
                            identifier.setValue(tokenParam.getValue());
                            identifier.setSystem(tokenParam.getSystem());
                            Patient patient = new Patient().addIdentifier(identifier);
                            exchange.getIn().setBody(patient);
                        })
                    .endDoTry()
                .endChoice()
                .otherwise()
                    .log("ABSOLUTE PatientId Type")
                .endChoice()
                
            .end()

            //If identifier not found; create dummy patient
            .choice()
                .when(simple("${header.CamelFhirPatientIdType} == 'IDENTIFIER' && ${header.CamelFhirServerPatientId} == null "))
                    .doTry()
                        .log("create  IDENTIFIER patient id  ${header." + CamelConstants.IDENTIFIER_OBJECT + "}")
                        .toD("fhir://create/resource?inBody=resource&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .process(exchange -> {
                            //Response may not be resource. It is OutCome
                            MethodOutcome response = exchange.getIn().getBody(MethodOutcome.class);
                            Patient patientResource = (Patient) response.getResource();
                            String serverPatientId = patientResource.getId().split("/_history")[0];
                            exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_RESOURCE, patientResource);
                            exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                        })
                        .log("Created server patient id  ${header." + CamelConstants.SERVER_PATIENT_ID + "}")

                    .doCatch(Exception.class)
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
                .when(header(CamelConstants.SERVER_PATIENT_ID).isNotNull())
                    .log("Server PatientId  exists ${header.CamelFhirServerPatientId}")
                .endChoice()
            .end()

            .process(exchange -> {
                //set back the input json as body
                String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                exchange.getIn().setBody(inputResource);
            })
            .log("FHIR PatientReferenceProcessor completed: input patient id: ${header." + CamelConstants.PATIENT_ID + "} input patient identifier:${header." + CamelConstants.IDENTIFIER_OBJECT + "}  and server patient id: ${header." + CamelConstants.SERVER_PATIENT_ID + "}");
   
        // Extract Patient Id from the FHIR Server response
        from("direct:extractPatientIdFromFhirResponseProcessor")
            .routeId("extractPatientIdFromFhirResponseProcessorRoute")
            // (From FhirUtils)
            // if Internal contained reference
            // or Transaction reference
            // Extract or find the Patient ID from the resource
            .choice()
                .when(header(CamelConstants.SERVER_PATIENT_ID).isNotNull())
                    .log("FHIR server Patient ID already set: ${header." + CamelConstants.SERVER_PATIENT_ID + "}")
                .otherwise()
                    .choice()
                        .when(header(CamelConstants.INPUT_RESOURCE_TYPE).isEqualTo("Bundle"))
                            .to("direct:extractPatientIdFromBundleResponse")
                        .otherwise()
                            .to("direct:extractPatientIdFromResourceResponse")
                        .endChoice()
                .endChoice();

        from ("direct:extractPatientIdFromBundleResponse")
            .routeId("extractPatientIdFromBundleResponseRoute")

            .doTry()
                .bean(PatientUtils.class, "getPatientIdFromResponse")
                
                .log("Read patient id  for ${header.CamelFhirServerPatientId}")
                .toD("fhir://read/resourceById?resourceClass=Patient&stringId=${header.CamelFhirServerPatientId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                .process(ServerPatientResourceProcessor.BEAN_ID)
            .doCatch(Exception.class)
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .log("FHIR server Bundle Response Patient ID: ${header." + CamelConstants.SERVER_PATIENT_ID + "}");

        from ("direct:extractPatientIdFromResourceResponse")
            .routeId("extractPatientIdFromResourceResponseRoute")
            .bean(PatientUtils.class, "getPatientIdFromOutCome")
            .log("FHIR server Patient ID: ${header." + CamelConstants.SERVER_PATIENT_ID + "}");

        // Extract Reference Resource Ids from the FHIR Input Resource
        // and update the Input  with Reference Internal Resource Ids
        from("direct:mapInternalResourceProcessor")
            .routeId("MapInternalResourceProcessor")

            // 1. Extract or find the Reference Resource ID(s) from the resource
            // 2. Check if the reference resource Id(s) already exist in the bundle or not.
            // If the reference resource Id(s) doesn't exist in the bundle.
            // Add it in the referenceResourceIds(excluding subject.reference)
            // Note : reference resource Id(s) are in the form of inputResourceId(s)
            .process(exchange -> {
                String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                List<String> referenceInputResourceIds = FhirUtils.getReferenceResourceIds(inputResource);
                exchange.setProperty(CamelConstants.REFERENCE_INPUT_RESOURCE_IDS, referenceInputResourceIds);
            })
            .log("FHIR Reference Resource ID(s) : ${exchangeProperty." + CamelConstants.REFERENCE_INPUT_RESOURCE_IDS + "}")
            // 3. Check the mapping table : FB_RESOURCE_COMPOSITION
            // and get the internalResourceId(s) for corresponding inputResourceId(s).
            // 4. Replace the reference inputResourceId(s) with the reference internalResourceId(s)
            // in the input fhir bundle
            .choice()
//                .when(exchangeProperty(CamelConstants.REFERENCE_INPUT_RESOURCE_IDS).isNotNull())
                .when(simple("${exchangeProperty." + CamelConstants.REFERENCE_INPUT_RESOURCE_IDS + "} != null && ${exchangeProperty." + CamelConstants.REFERENCE_INPUT_RESOURCE_IDS + ".size()} > 0"))
                .log("Reference Resource IDs: ${exchangeProperty." + CamelConstants.REFERENCE_INPUT_RESOURCE_IDS + "}")
                    .process(ResourceLookupProcessor.BEAN_ID)
            .end()
            .log("FHIR INTERNAL RESOURCE ID(s) : ${exchangeProperty." + CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS + "}")
            .log("Updated input resouce bundle with the internalResourceIds");

        // Add the extracted Reference Resource Ids as resource in the FHIR Input Bundle Resource
        from("direct:resourceReferenceProcessor")
            .routeId("ResourceReferenceProcessorRoute")
            // 1. Fetch the resources for the internalResourceId(s) is/are in the server.
            // 2. Add the resources in the input fhir bundle.
            .choice()
//                .when(exchangeProperty(CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS).isNotNull())
                .when(simple("${exchangeProperty." + CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS + "} != null && ${exchangeProperty." + CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS + ".size()} > 0"))

                    .log("Property " + CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS + " is present.")
                    // Process the list of internal resource IDs
                    .process(exchange -> {
                        // Retrieve the list of internal resource Ids from property
                        List<String> resourceIds = exchange.getProperty(CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS, List.class);
                        // Add the list to the exchange body for splitting
                        exchange.getIn().setBody(resourceIds);
                        // Initialize the list of existingResources before split
                        List<String> existingResources = exchange.getProperty(CamelConstants.SERVER_EXISTING_RESOURCES, List.class);
                        if (existingResources == null) {
                            existingResources = new ArrayList<>();
                            exchange.setProperty(CamelConstants.SERVER_EXISTING_RESOURCES, existingResources);
                        }
                    })
                    // Split the list to process each resource Ids individually
                    .split(body()).shareUnitOfWork()
                        .process(exchange -> {
                            // Extract list of internal resource Ids from the current body string
                            String resourceString = exchange.getIn().getBody(String.class);
                            // Use regular expression to extract resourceType and internal id
                            Pattern pattern = Pattern.compile("([^/]+)/([^/]+)");
                            Matcher matcher = pattern.matcher(resourceString);
                            if (matcher.matches()) {
                                exchange.setProperty(CamelConstants.INTERNAL_RESOURCE_TYPE, matcher.group(1));
                                exchange.setProperty(CamelConstants.STRING_INTERAL_ID, matcher.group(2));
                            }
                        })
                        .log("Processing resource: Class = ${exchangeProperty." + CamelConstants.INTERNAL_RESOURCE_TYPE + "}, ID = ${exchangeProperty." + CamelConstants.STRING_INTERAL_ID + "}")
                        .doTry()
                            .toD("fhir:read/resourceById?resourceClass=${exchangeProperty.FhirServerResourceType}&stringId=${exchangeProperty.FhirServerExistingId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                            .process(ExistingServerResourceProcessor.BEAN_ID)
                        .doCatch(ResourceNotFoundException.class)
                            .log("Resource not found for resourceClass=${exchangeProperty.resourceClass}, stringId=${exchangeProperty.stringId}. Skipping...")
                        .endDoTry()
                    .end()
            .end()
            .process(ExistingResourceReferenceProcessor.BEAN_ID)
            .log("Updated input resouce bundle with the referece resources");

        from("direct:checkDuplicateResource")
                .routeId("CheckDuplicateResource")

                // 1. Retrieve the list of all input resource ids
                .process(exchange -> {
                    String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
                    List<String> inputResourceIds = FhirUtils.getInputResourceIds(inputResource);
                    exchange.setProperty(CamelConstants.INPUT_RESOURCE_IDS, inputResourceIds);
                })
                .log("FHIR Input Resource ID(s) : ${header." + CamelConstants.INPUT_RESOURCE_IDS + "}")
                // 2. Check the mapping table : FB_RESOURCE_COMPOSITION
                // and get the compositionId(s) for corresponding inputResourceId(s).
                // 3. If all compositionId(s) is/are same throw duplicate bundle resource exception.
                .choice()
//                    .when(exchangeProperty(CamelConstants.INPUT_RESOURCE_IDS).isNotNull())
                    .when(simple("${exchangeProperty." + CamelConstants.INPUT_RESOURCE_IDS + "} != null && ${exchangeProperty." + CamelConstants.INPUT_RESOURCE_IDS + ".size()} > 0"))

                        .log("Input Resource IDs: ${header." + CamelConstants.INPUT_RESOURCE_IDS + "}")
                        .doTry()
                            .process(CompositionLookupProcessor.BEAN_ID)
                        .doCatch(Exception.class)
                            .log("CheckDuplicateResource: CompositionLookupProcessor catch exception")
                            .process(new FhirBridgeExceptionHandler())
                        .endDoTry()
                .endChoice()
                .log("CheckDuplicateResource: No duplicate resources found in the input resource or input bundle");

        from("direct:deleteResources")
                .process(exchange -> {
                    // Get the list of resource URLs from the Exchange property
                    List<String> resourceUrls = exchange.getProperty(CamelConstants.INPUT_RESOURCE_IDS, List.class);

                    // Set the body with the list of resource URLs for processing
                    exchange.getIn().setBody(resourceUrls);
                })
                .split(body())  // Iterate over each resource URL
                .doTry()
                    .process(exchange -> {
                        // Extract resource type and ID from the URL
                        String resourceUrl = exchange.getIn().getBody(String.class);
                        String[] parts = resourceUrl.split("/");

                        if (parts.length == 2) {
                            exchange.getIn().setHeader("type", parts[0]);       // Resource Type (e.g., "Patient")
                            exchange.getIn().setHeader("stringId", parts[1]);  // Resource ID (e.g., "123")
                        } else {
                            throw new IllegalArgumentException("Invalid FHIR resource URL: " + resourceUrl);
                        }
                    })
                    .toD("fhir://delete/resourceById?serverUrl={{serverUrl}}&type=${header.type}&stringId=${header.stringId}")
                    .log("Deleted FHIR resource: ${header.type}/${header.stringId}")
                .doCatch(Exception.class)
                    .log("Failed to delete FHIR resource: ${header.type}/${header.stringId} - Exception: ${exception.message}")
                    .process(new FhirBridgeExceptionHandler())
                .end();
    }
}

