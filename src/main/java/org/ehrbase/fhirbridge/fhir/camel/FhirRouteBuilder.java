package org.ehrbase.fhirbridge.fhir.camel;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.route.AbstractRouteBuilder;
import org.ehrbase.fhirbridge.exception.FhirBridgeExceptionHandler;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.ehrbase.fhirbridge.fhir.support.PatientUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@SuppressWarnings("java:S1192")
public class FhirRouteBuilder extends AbstractRouteBuilder {
    private final ObjectMapper objectMapper;

    public FhirRouteBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure() throws Exception {
        from("direct:FHIRProcess")
            .choice()
                .when(header(CamelConstants.REQUESTDETAILS_OPERATION_TYPE).isEqualTo("CREATE"))
                    .to("direct:handleCreateOperation")
                .when(header(CamelConstants.REQUESTDETAILS_OPERATION_TYPE).isEqualTo("READ"))
                    .to("direct:handleReadOperation")
                .otherwise()
                    .log("Unknown operation type: ${header.FHIR_REQUEST_DETAILS}")
                .endChoice()
            .end();       

        from("direct:handleReadOperation")
            .log("handleReadOperation")
            .doTry()
                .log("Read resource for ${header.CamelRequestResourceResourceType}/${header.CamelRequestDetailsId}")
                .toD("fhir://read/resourceById?resourceClass=${header.CamelRequestResourceResourceType}&stringId=${header.CamelRequestDetailsId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                
                //Store the response in the Exchange
                .process(exchange -> {
                    exchange.getIn().setHeader(CamelConstants.REQUEST_HTTP_METHOD, "GET");

                    //Response may not be resource. It is OutCome
                    Resource response = (Resource) exchange.getIn().getBody();
                    exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME,  response);
                })
            .log("FHIR request to Read Resource ${header.CamelRequestResourceResourceType}/${header.CamelRequestDetailsId} completed ${body}")
      
            .doCatch(Exception.class)
                .log("direct:handleReadOperation  exception during fhir read")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end();
        
        
        from("direct:handleCreateOperation")
            // Forward request to FHIR server
                .choice()
                    .when(simple("${header.CamelRequestResourceResourceType} == 'Bundle'"))
                        // if body.type == "transaction"
                        .process(exchange -> {
                            //Set the incoming resource in body as camel-fhir take from body (inBody)
                            exchange.getIn().setBody(exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE));
                        })
                        // create Transaction bundle in our FHIR server
                        .log("Transaction FHIR request. Starting process...")
                        
                        .doTry()
                            .to("fhir://transaction/withBundle?inBody=bundle&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                            //Store the response in the Exchange
                            .process(exchange -> {
                                //Response may not be resource. It is OutCome
                                Bundle response = exchange.getIn().getBody(Bundle.class);
                                exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, response);

                                 //set back the input json as body
                                Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
                                exchange.getIn().setBody(inputResource);
                                                
                            })
                        .doCatch(Exception.class)
                            .log("direct:FHIRProcess fhir://transaction exception")
                            .process(new FhirBridgeExceptionHandler())
                        .endDoTry()

                    .endChoice()
                    .when(simple("${header.CamelRequestResourceResourceType} != 'Bundle' && ${header.CamelHttpMethod} == 'POST'"))
                    .doTry()
                        .log("Create FHIR request. Starting process...")
                        .to("fhir://create/resource?inBody=resource&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .log("Create FHIR request. Done...")
                        
                        //Store the response in the Exchange
                        .process(exchange -> {
                            exchange.getIn().setHeader(CamelConstants.REQUEST_HTTP_METHOD, "POST");

                            //Response may not be resource. It is OutCome
                            MethodOutcome response = exchange.getIn().getBody(MethodOutcome.class);
                            exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, response);

                            //set back the input json as body
                            Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
                            exchange.getIn().setBody(inputResource);
                                        
                        })
                    .doCatch(Exception.class)
                        .log("direct:FHIRProcess  exception during fhir create")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                    .endChoice()
                    .when(simple("${header.CamelRequestResourceResourceType} != 'Bundle' && ${header.CamelHttpMethod} == 'PUT'"))
                    .doTry()

                        .log("Update FHIR request. Starting process...")
                        .to("fhir://update/resource?inBody=resource&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .log("Update FHIR request. Done...")
 
                        //Store the response in the Exchange
                        .process(exchange -> {
                            exchange.getIn().setHeader(CamelConstants.REQUEST_HTTP_METHOD, "PUT");

                            //Response may not be resource. It is OutCome
                            MethodOutcome response = exchange.getIn().getBody(MethodOutcome.class);
                            exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, response);

                            //set back the input json as body
                            Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
                            exchange.getIn().setBody(inputResource);
                                        
                        })
                    .doCatch(Exception.class)
                        .log("direct:FHIRProcess exception during fhir update")
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                    .endChoice()
                .otherwise()
                    // else create Resource in our FHIR server
                    .log("Unsupported operation...")
                .end()
                .log("FHIR request processed by FHIR server");
        
        // Util: Extract: 
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
            .log("FHIR PatientId ${header." + CamelConstants.FHIR_INPUT_PATIENT_ID + "}" );

        //Util: Extract  for all falvours of patientId(relative, identifier, absolute
        // and validate if it exists in the fhir server
        from("direct:extractAndValidatePatientIdExistsProcessor")
            .routeId("extractAndCheckPatientIdExistsProcessorRoute")
            //Get the patientid from input resource(Bundle, Patient or any resource)
            //find the patient id in the fhir server
            
            // Extract or find the Patient ID from the resource 
            // and get the server patient id from db if present
            .doTry()
                .bean(PatientUtils.class, "extractPatientIdOrIdentifier")
            .doCatch(Exception.class)
                .log("extractPatientIdOrIdentifier exception")
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .end()

            .log("FHIR PatientId ${header." + CamelConstants.FHIR_INPUT_PATIENT_ID 
                    + "}, Server PatientId ${header." 
                    + CamelConstants.FHIR_SERVER_PATIENT_ID 
                    + "}, PatientId Type ${header."
                    + CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE
                    + "}")

            .choice()
            .when(header(CamelConstants.FHIR_SERVER_PATIENT_ID).isNull())
                .to("direct:checkPatientIdExistsProcessor");
        
        //Extract    
        from ("direct:checkPatientIdExistsProcessor")
            .routeId("checkPatientIdExistsProcessorRoute")
            
            // Cannot throw ResourceNotFound here. As the patient can be created 
            // Internal Reference : verify the id is in the server: update the mapper table
            // Internal contained reference : get the sever id after the respource is created and then update themapper table
            // Transaction reference : get the sever id after the respource is created and then update themapper table
            // External reference (absolute URL) : add to the mapper table as is
                
            // If patient is found in server (PatientResourceFromServer) store this patientId 
            // as serverPatientId in exchange so that it can be added to fhi-patient-id to ehr-id map table
            //else it has to be created after the resource is created in the server.
            .choice()
                .when(header(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE).isEqualTo("RELATIVE_REFERENCE"))
                    .doTry()
                        .log("Read RELATIVE_REFERENCE patient id ${header.CamelFhirPatientId}")
                        .toD("fhir://read/resourceById?resourceClass=Patient&stringId=${header.CamelFhirPatientId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .process(ExistingServerPatientResourceProcessor.BEAN_ID)
                    .doCatch(Exception.class)
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
                //else if identifier
                .when(header(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE).isEqualTo("IDENTIFIER"))
                    .doTry()
                        .log("Search IDENTIFIER patient id for identifier ${header." + CamelConstants.IDENTIFIER_STRING + "}")
                        .toD("fhir://search/searchByUrl?url=Patient?identifier=${header.CamelIdentifierString}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .log("Response Search patient id  for identifier ${body}")
                        .bean(PatientUtils.class, "extractPatientIdentifier")
                        .log("Search server IDENTIFIER patient id ${header." + CamelConstants.FHIR_SERVER_PATIENT_ID + "}")
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
                .when(header(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE).isEqualTo("SEARCH_URL"))
                    .doTry()
                        .log("Read SEARCH_URL patient id ${header.CamelFhirPatientId}") 
                        .toD("fhir://search/searchByUrl?url=${header.CamelFhirPatientId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
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
                                        throw new UnprocessableEntityException("Patient not found for search url: " + exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_SEARCH_URL));
                                    } else {
                                        String serverPatientId = "Patient/" + serverPatient.getIdPart();
                                        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE, serverPatient);
                                        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
                                        exchange.getIn().setBody(serverPatient);
                                    }
                                } else {
                                    throw new UnprocessableEntityException("Patient not found for search url: " + exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_SEARCH_URL));
                                }
                            })            
                    .doCatch(Exception.class)
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
                .otherwise()
                    .log("ABSOLUTE PatientId Type")
                .endChoice()
                
            .end()

            //If identifier not found; create dummy patient
            .choice()
                .when(simple("${header.CamelFhirInputPatientIdType} == 'IDENTIFIER' && ${header.CamelFhirServerPatientId} == null "))
                    .doTry()
                        .log("create  IDENTIFIER patient id  ${header." + CamelConstants.IDENTIFIER_OBJECT + "}")
                        .toD("fhir://create/resource?inBody=resource&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                        .process(exchange -> {
                            //Response may not be resource. It is OutCome
                            MethodOutcome response = exchange.getIn().getBody(MethodOutcome.class);
                            Patient patientResource = (Patient) response.getResource();
                            String serverPatientId = patientResource.getId().split("/_history")[0];
                            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE, patientResource);
                            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
                        })
                        .log("Created server patient id  ${header." + CamelConstants.FHIR_SERVER_PATIENT_ID + "}")

                    .doCatch(Exception.class)
                        .process(new FhirBridgeExceptionHandler())
                    .endDoTry()
                .endChoice()
                .when(header(CamelConstants.FHIR_SERVER_PATIENT_ID).isNotNull())
                    .log("Server PatientId  exists ${header.CamelFhirServerPatientId}")
                .endChoice()
            .end()

            .process(exchange -> {
                //set back the input json as body
                Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
                exchange.getIn().setBody(inputResource);
            })
            .log("FHIR PatientReferenceProcessor completed: input patient id: ${header." + CamelConstants.FHIR_INPUT_PATIENT_ID + "} input patient identifier:${header." + CamelConstants.IDENTIFIER_OBJECT + "}  and server patient id from db: ${header." + CamelConstants.FHIR_SERVER_PATIENT_ID + "}");

        // Extract Reference(all  flavours: local, relative, absolute, or internal) Resource Ids 
        // from the FHIR Input Resource, lookup db and maintain the mapping
        from("direct:mapReferencedInternalResourceProcessor")
            .routeId("MapReferencedInternalResourceProcessor")

            // 1. Extract or find the Reference Resource ID(s) from the resource
            // 2. Check if the reference resource Id(s) already exist in the bundle or not.
            // If the reference resource Id(s) doesn't exist in the bundle.
            // Add it in the referenceResourceIds(excluding subject.reference)
            // Note : reference resource Id(s) are in the form of inputResourceId(s)
            .process(exchange -> {
                Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
                List<String> referenceInputResourceIds = FhirUtils.getReferenceResourceIds(inputResource);
                exchange.setProperty(CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS, referenceInputResourceIds);
            })
            // 3. Check the mapping table : FB_RESOURCE_COMPOSITION
            // and get the internalResourceId(s) for corresponding inputResourceId(s).
            // 4. Replace the reference inputResourceId(s) with the reference internalResourceId(s)
            // in the input fhir bundle
            .choice()
                .when(simple("${exchangeProperty." + CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS + "} != null && ${exchangeProperty." + CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS + ".size()} > 0"))
                .log("FHIR Reference Resource IDs: ${exchangeProperty." + CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS + "}")
                    .process(ReferencedResourceLookupProcessor.BEAN_ID)
            .end()
            .log("FHIR Internal Resource ID(s) : ${exchangeProperty." + CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS + "}")
            .log("Updated input resouce bundle with the internalResourceIds");

        // once input is sent to FHIR server(ResourcePersistenceProcessor), this processor will be called to 
        // Extract Patient Id Patient resource and  resourceid 
        // This is needed to maintain the ids against compositionid in db
        // and the resource to be sent to opeFHIR
        from("direct:extractPatientIdFromFhirResponseProcessor")
            .routeId("extractPatientIdFromFhirResponseProcessorRoute")
            // (From FhirUtils)
            // if Internal contained reference
            // or Transaction reference
            // Extract or find the Patient ID from the resource
            .choice()
                .when(header(CamelConstants.REQUEST_RESOURCE_TYPE).isEqualTo("Bundle"))
                    .to("direct:extractPatientIdAndResourceIdFromBundleResponse")
            .otherwise()
                    .to("direct:extractPatientIdAndResourceIdFromResourceResponse")
            .endChoice()
            .end();

        // Extract Patient Id Patient resource and  resourceid 
        from ("direct:extractPatientIdAndResourceIdFromBundleResponse")
            .routeId("extractPatientIdAndResourceIdFromBundleResponseRoute")

            .doTry()
                .bean(PatientUtils.class, "getPatientIdAndResourceIdFromResponse")
                //Get the patient resource
                //This is needed to create EHRId in EHRBase for the patient identifier
                .log("Read patient id  for ${header.CamelFhirServerPatientId}")
                .toD("fhir://read/resourceById?resourceClass=Patient&stringId=${header.CamelFhirServerPatientId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                .process(ServerPatientResourceProcessor.BEAN_ID)
            .doCatch(Exception.class)
                .process(new FhirBridgeExceptionHandler())
            .endDoTry()
            .log("FHIR Bundle Response server Patient ID: ${header." + CamelConstants.FHIR_SERVER_PATIENT_ID + "}");

        // Extract Patient Id Patient resource and  resourceid 
        from ("direct:extractPatientIdAndResourceIdFromResourceResponse")
            .routeId("extractPatientIdAndResourceIdFromResourceResponseRoute")
            .bean(PatientUtils.class, "getPatientIdAndResourceIdFromOutCome")
            //TODO: Need to get the FHIR_SERVER_PATIENT_RESOURCE if the resource is not Patient resource?
            .log("FHIR Resource Response server Patient ID: ${header." + CamelConstants.FHIR_SERVER_PATIENT_ID + "}");


        //Validate and Enrich
        // Add the extracted Reference Resource Ids as resource in the FHIR Input Bundle Resource
        from("direct:referencedResourceProcessor")
            .routeId("ReferencedResourceProcessorRoute")
            // 1. Fetch the resources for the internalResourceId(s) is/are in the server.
            // 2. Add the resources in the input fhir bundle.
            .choice()
                .when(simple("${exchangeProperty." + CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS + "} != null && ${exchangeProperty." + CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS + ".size()} > 0"))

                    .log("Property " + CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS + " is present.")
                    // Process the list of internal resource IDs
                    .process(exchange -> {
                        // Retrieve the list of internal resource Ids from property
                        Object property = exchange.getProperty(CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS, List.class);
                        List<String> resourceIds = objectMapper.convertValue(
                                property, new TypeReference<>() {}
                        );
                        // Add the list to the exchange body for splitting
                        exchange.getIn().setBody(resourceIds);
                        // Initialize the list of existingResources before split
                        Object exchangeProperty = exchange.getProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, List.class);
                        List<String> existingResources = objectMapper.convertValue(
                                exchangeProperty, new TypeReference<>() {}
                        );
                        if (existingResources == null) {
                            existingResources = new ArrayList<>();
                            exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, existingResources);
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
                                exchange.setProperty(CamelConstants.FHIR_INTERNAL_RESOURCE_TYPE, matcher.group(1));
                                exchange.setProperty(CamelConstants.FHIR_INTERAL_EXISTING_ID, matcher.group(2));
                            }
                        })
                        .log("Processing resource: Class = ${exchangeProperty." + CamelConstants.FHIR_INTERNAL_RESOURCE_TYPE + "}, ID = ${exchangeProperty." + CamelConstants.FHIR_INTERAL_EXISTING_ID + "}")
                        .doTry()
                            .toD("fhir:read/resourceById?resourceClass=${exchangeProperty.CamelFhirInternalResourceType}&stringId=${exchangeProperty.CamelFhirInternalExistingId}&serverUrl={{serverUrl}}&fhirVersion={{fhirVersion}}")
                            .process(ExistingServerResourceProcessor.BEAN_ID)
                        .doCatch(ResourceNotFoundException.class)
                            .log("Resource not found for resourceClass=${exchangeProperty.resourceClass}, stringId=${exchangeProperty.stringId}. Skipping...")
                        .endDoTry()
                    .end()
            .end()
            .process(ExistingResourceReferenceProcessor.BEAN_ID)
            .log("Updated input resouce bundle with the referece resources");

 
        from("direct:deleteResources")
                .process(exchange -> {
                    // Get the list of resource URLs from the Exchange property
                    Object property = exchange.getProperty(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, List.class);
                    List<String> resourceUrls = objectMapper.convertValue(
                            property, new TypeReference<>() {}
                    );

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

