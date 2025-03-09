package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Component
public class PatientUtils {

    public static final String RESOURCE_TYPE = "resourceType";
    public static final String IDENTIFIER = "identifier";
    public static final String SYSTEM = "system";
    public static final String VALUE = "value";
    public static final String PATIENT = "Patient";
    public static final String ENTRY = "entry";
    public static final String FULL_URL = "fullUrl";
    public static final String CONTAINED = "contained";
    private PatientEhrRepository patientEhrRepository;

    private  final ObjectMapper objectMapper = new ObjectMapper();

    public PatientUtils(PatientEhrRepository patientEhrRepository) {
        this.patientEhrRepository = patientEhrRepository;
    }

    @Handler
    public  void extractPatientIdOrIdentifier(Exchange exchange) {
        // Parse the JSON
        String  resourceJson = exchange.getIn().getBody(String.class);
        JsonNode resourceNode = getPatientInfoResource(resourceJson);

        String patientId = null;
        String serverPatientId = null;
        if (resourceNode.has(RESOURCE_TYPE) && PATIENT.equals(resourceNode.get(RESOURCE_TYPE).asText())) {
            //Patient resource
            //get FHIR_INPUT_PATIENT_ID and FHIR_SERVER_PATIENT_ID
            //TODO: Should this be id or identifier value??
            // Get the "identifier" array
            JsonNode identifierNode = resourceNode.path(IDENTIFIER);

            // Access the first identifier object in the array (assuming it's the first one)
            JsonNode firstIdentifier = identifierNode.isArray() && identifierNode.size() > 0 ? identifierNode.get(0) : null;

            if (firstIdentifier != null) {
                // Extract "system" and "value" from the first identifier
                String system = firstIdentifier.path(SYSTEM).asText();
                String value = firstIdentifier.path(VALUE).asText();
                patientId = system + "|" + value;
            }
            
            serverPatientId = getServerPatientIdFromDb(patientId);
            if (serverPatientId != null)  {
                //This can be the case when Patient resource is part of the bundle and the patient is already created
                throw new UnprocessableEntityException(resourceNode.path(RESOURCE_TYPE) + " absolute reference: " + patientId + " already exists.Please provide relative reference: " + serverPatientId);
            }
            //Set ids in exchange
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "ABSOLUTE_PATIENT");

            return;
        }
        
        //subject, individual, patient resource
        //get FHIR_INPUT_PATIENT_ID or IDENTIFIER_OBJECT
        if (!resourceNode.isMissingNode()) {
            // Check for "reference" field in subject
            if (resourceNode.has("reference")) {
                String reference = resourceNode.get("reference").asText();
                
                //get FHIR_INPUT_PATIENT_ID and FHIR_SERVER_PATIENT_ID
                patientId = extractPatientIdFromReference(exchange, resourceNode, reference);
                serverPatientId = getServerPatientIdFromDb(patientId);

                //Set ids in exchange
                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
                                
            } else if (resourceNode.has(IDENTIFIER)) {
                //get the Identifier systema nd value
                String system =  resourceNode.path(IDENTIFIER).path(SYSTEM).asText();
                String value =  resourceNode.path(IDENTIFIER).path(VALUE).asText();
                
                //TODO: Adding identifier as well in the PatientEhr repository
                //get FHIR_INPUT_PATIENT_ID and FHIR_SERVER_PATIENT_ID
                patientId = system + "|" + value;
                serverPatientId = getServerPatientIdFromDb(patientId);
                
                //Set in exchange
                //Set ids in exchange
                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);

                exchange.setProperty(CamelConstants.IDENTIFIER_OBJECT, new TokenParam(system, value));
                exchange.getIn().setHeader(CamelConstants.IDENTIFIER_STRING,value);
                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "IDENTIFIER");
            }   
        }
    }

    public JsonNode getPatientInfoResource(String resourceJson) {
        //PatientReferenceProcessor
        try {
            // Parse the JSON
            JsonNode rootNode = objectMapper.readTree(resourceJson);

            // Check if the resource is a Bundle
            if (rootNode.has(RESOURCE_TYPE) && "Bundle".equals(rootNode.get(RESOURCE_TYPE).asText())) {
                // Handle as a Bundle
                JsonNode entryArray = rootNode.path(ENTRY);
                for (JsonNode entryNode : entryArray) {
                    // Get the resource in each entry
                    JsonNode resourceNode = entryNode.path("resource");
                    JsonNode patientInfo = extractPatientFromResource(resourceNode);
                    if (patientInfo != null) {
                        return patientInfo;
                    }
                }
            } else {
                // Handle as a single resource
                return extractPatientFromResource(rootNode);
            }
        } catch (Exception e) {
            throw new UnprocessableEntityException("Unable to process the resource JSON and failed to extract patient information from the provided JSON.");
        }

        return null; // Return null if no patient ID is found
    }

    public JsonNode extractPatientFromResource(JsonNode resourceNode) {
        // If the resource itself is a Patient, return its ID
        if (resourceNode.has(RESOURCE_TYPE) && PATIENT.equals(resourceNode.get(RESOURCE_TYPE).asText())) {
            return resourceNode;
        }

        JsonNode referenceNode;
        String resourceType = resourceNode.path(RESOURCE_TYPE).asText();
        // patient(Consent, Immunization) individual(ResearchSubject)
        // Determine the reference node(subject or individual) based on resource type
        if ("ResearchSubject".equals(resourceType)) {
            referenceNode = resourceNode.path("individual");
        } else if ("Consent".equals(resourceType) || "Immunization".equals(resourceType)) {
            referenceNode = resourceNode.path("patient");
        } else {
            // Look for the "subject" field within the resource
            referenceNode = resourceNode.path("subject");
        }

        if (!referenceNode.isMissingNode()) {
                return referenceNode;
            
        } else {
            throw new UnprocessableEntityException(resourceNode.path(RESOURCE_TYPE) + " should be linked to a subject/patient");
        }

    }

    public  String extractPatientIdFromReference(Exchange exchange, JsonNode resourceNode, String reference) {
        if (reference != null) {
            if (reference.startsWith("Patient/")) {
                //Internal Reference
                // Relative reference to Patient resource
                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "RELATIVE_REFERENCE");
                return reference;
            } else if (reference.startsWith("#")) {
                // Internal contained reference
                //The contained id will be created and an id will be 
                //returned by the server after the resource is created
                // Contained:
                // {
                // "subject": {
                //     "reference": "#patient1"
                // },
                // "contained": [
                //     {
                //     "resourceType": "Patient",
                //     "id": "patient1",
                //     "name": [
                //         {
                //         "family": "Doe",
                //         "given": ["John"]
                //         }
                //     ]
                //     }
                // ]
                // }

                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "ABSOLUTE_REFERENCE");
                String referenceStr = reference.split("#")[1];
                JsonNode resource = extractContainedResource(resourceNode, referenceStr);
                if (resource != null) {
                    return referenceStr;
                }

                return null;

            } else if (reference.startsWith("urn:uuid")) {
                // Transaction reference
                //TODO Check what needs to be done here
                //The transaction id will be created and an id will be 
                //returned by the server after the resource is created
                // Transaction:
                // Observation resource references the Patient using a temporary urn:uuid:.
                // "subject": {
                // "reference": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"
                // },

                // Patient in a single transaction bundle. 
                // "fullUrl": "urn:uuid:123e4567-e89b-12d3-a456-426614174000",
                // "resource": {
                //     "resourceType": "Patient",
                //     "id": "example_patient",
                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "ABSOLUTE_REFERENCE");
                JsonNode resource = extractFullUrlResource(resourceNode, reference);
                if (resource != null) {
                    return resource.path("resource").path("id").asText();
                }
                return null;
            } else {
                // External reference (absolute URL)
                // External/absolute url:
                // {
                // "subject": {
                //     "reference": "http://external-fhir-server.com/Patient/456"
                // }
                // }

                //create ehrid for this "http://external-fhir-server.com/Patient/456
                //maintain mapping between the patientid, ehrid record.
                //corresponding compositionid

                //If FHIR server is not able to resolve the external ref, 404 is returned
                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "EXTERNAL_REFERENCE");
                return reference;
            }
        }
        return null;
    }

    public String getServerPatientIdFromDb(String patientId) {

        //get the internalPatientId from db
        String internalPatientId = Optional.ofNullable(patientEhrRepository.findByInputPatientId(patientId))  
                                    .map(PatientEhr::getInternalPatientId)  
                                    .orElseGet(() -> Optional.ofNullable(patientEhrRepository.findByInternalPatientId(patientId))
                                    .map(PatientEhr::getInternalPatientId)  
                                    .orElse(null));  
        
        return internalPatientId;
    }

    public void getPatientIdAndResourceIdFromOutCome(Exchange exchange) {
        String serverPatientId = (String) exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID);
        MethodOutcome resource = (MethodOutcome) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
        String resourceType = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE);
        
        //Get the Resource ID from the Outcome
        String serverResourceId = resource.getId().getResourceType() + "/" + resource.getId().getIdPart();
        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID, serverResourceId);

        //get patient id if not present
        if (serverPatientId == null) {
            if ("Patient".equals(resourceType)) {
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverResourceId);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE, resource.getResource());
            } else {
                // get the subject reference 
                serverPatientId = (String) exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
            }
        }
    }

    public void getPatientIdFromPatientResource(Exchange exchange) {
        String responseString = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
        String patientId = null;
        try {
            JsonNode rootNode = objectMapper.readTree(responseString);
             // Parse the JSON string into a JsonNode
            
            // Get the "identifier" array
            JsonNode identifierNode = rootNode.path(IDENTIFIER);

            // Access the first identifier object in the array (assuming it's the first one)
            JsonNode firstIdentifier = identifierNode.isArray() && identifierNode.size() > 0 ? identifierNode.get(0) : null;

            if (firstIdentifier != null) {
                // Extract "system" and "value" from the first identifier
                String system = firstIdentifier.path(SYSTEM).asText();
                String value = firstIdentifier.path(VALUE).asText();
                patientId = system + "|" + value;
            }
            String serverPatientId = getServerPatientIdFromDb(patientId);
            if (serverPatientId != null)  {
                //This can be the case when Patient resource is part of the bundle and the patient is already created
                throw new UnprocessableEntityException("Patient: " + patientId + " already exists");
            }
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            throw new UnprocessableEntityException("Unable to process the provided Patient resource JSON");
        }
    }

    public void getPatientIdAndResourceIdFromResponse(Exchange exchange) {
        String serverPatientId = (String) exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID);
        String responseString = (String) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);

        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(responseString);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        //Get the first Resource id from the response
        //TODO: check if we need to add all the resourceids in the feeder audit
        Optional<String> serverResourceId = Optional.ofNullable(rootNode.get(ENTRY))
                            .map(entryNode -> entryNode.elements().next())
                            .map(firstEntry -> firstEntry.path("response").path("location").asText()); 
        
        serverResourceId.ifPresent( id ->
            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID, id)
        );

        if (serverPatientId != null){
            //patient id already found. so return
            // via reference id
            return;
        }

        //Get Patient Id from response
        // Extract the location if it starts with "Patient/"
        Optional<String> patientLocation = Optional.ofNullable(rootNode.get(ENTRY))
                .map(entryNode -> entryNode.elements())  // Get the array of entry
                .map(entryIterator -> {
                    // Convert iterator to stream
                    return StreamSupport.stream(((Iterable<JsonNode>) () -> entryIterator).spliterator(), false)
                            .map(entry -> {
                                JsonNode response = entry.path("response"); 
                                String location = response.path("location").asText(null); 

                                // Check if the location starts with "Patient/"
                                if (location != null && location.startsWith("Patient/")) {
                                    // Truncate the location till before "/_history"
                                    int index = location.indexOf("/_history");
                                    if (index != -1) {
                                        location = location.substring(0, index); 
                                    }
                                    return location; 
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .findFirst(); 
                })
                .orElse(Optional.empty());
                
            if (patientLocation.isPresent()) {
                serverPatientId = patientLocation.get();
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
            }
    }

    public static Identifier getPseudonym(Patient patient) {
        return patient.getIdentifier().stream()
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Patient must have an identifier"));
    }

    private JsonNode extractFullUrlResource(JsonNode rootNode, String fullUrl) {
        JsonNode resource = null;

        // Directly target the `entry` array
        if (rootNode.has(ENTRY) && rootNode.get(ENTRY).isArray()) {
            for (JsonNode entry : rootNode.get(ENTRY)) {
                if (entry.has(FULL_URL) && entry.get(FULL_URL).isTextual() && fullUrl.equals(entry.get(FULL_URL).asText())) {
                    resource = entry.deepCopy();
                }
            }
        }
        return resource;
    }

    private JsonNode extractContainedResource(JsonNode rootNode, String containedId) {
        JsonNode resource = null;

        // Directly target the `contained` array
        if (rootNode.has(CONTAINED) && rootNode.get(CONTAINED).isArray()) {
            for (JsonNode contained : rootNode.get(CONTAINED)) {
                if (contained.has(RESOURCE_TYPE)
                && contained.get(RESOURCE_TYPE).isTextual()
                && PATIENT.equals(contained.get(RESOURCE_TYPE).asText())
                && contained.has("id") 
                && contained.get("id").isTextual() 
                && containedId.equals(contained.get("id").asText())) {
                    resource = contained.deepCopy();
                }
            }
        }
        return resource;
    }

}
