package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import java.util.Optional;
import java.util.stream.StreamSupport;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PatientUtils {

    private PatientEhrRepository patientEhrRepository;

    private  final ObjectMapper objectMapper = new ObjectMapper();

    private PatientUtils(PatientEhrRepository patientEhrRepository) {
        this.patientEhrRepository = patientEhrRepository;
    }

    @Handler
    public  void extractPatientIdOrIdentifier(Exchange exchange) {
        // Parse the JSON
        String  resourceJson = exchange.getIn().getBody(String.class);
        JsonNode resourceNode = getPatientInfoResource(resourceJson);

        String patientId = null;
        String serverPatientId = null;
        if (resourceNode.has("resourceType") && "Patient".equals(resourceNode.get("resourceType").asText())) {
            //Patient resource
            //get PATIENT_ID and SERVER_PATIENT_ID
            patientId =  resourceNode.path("id").asText();
            serverPatientId = getServerPatientIdFromDb(exchange, patientId);

            //Set ids in exchange
            exchange.getIn().setHeader(CamelConstants.PATIENT_ID, patientId);
            exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
            exchange.getIn().setHeader(CamelConstants.PATIENT_ID_TYPE, "ABSOLUTE_PATIENT");

            return;
        }
        
        //subject, individual, patient resource
        //get PATIENT_ID or IDENTIFIER_OBJECT
        if (!resourceNode.isMissingNode()) {
            // Check for "reference" field in subject
            if (resourceNode.has("reference")) {
                String reference = resourceNode.get("reference").asText();
                
                //get PATIENT_ID and SERVER_PATIENT_ID
                patientId = extractPatientIdFromReference(exchange, resourceNode, reference);
                serverPatientId = getServerPatientIdFromDb(exchange, patientId);

                //Set ids in exchange
                exchange.getIn().setHeader(CamelConstants.PATIENT_ID, patientId);
                exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                                
            } else if (resourceNode.has("identifier")) {
                //get the Identifier systema nd value
                String system =  resourceNode.path("identifier").path("system").asText();
                String value =  resourceNode.path("identifier").path("value").asText();
                
                //TODO: Adding identifier as well in the PatientEhr repository
                //get PATIENT_ID and SERVER_PATIENT_ID
                patientId = system + "|" + value;
                serverPatientId = getServerPatientIdFromDb(exchange, patientId);
                
                //Set in exchange
                //Set ids in exchange
                exchange.getIn().setHeader(CamelConstants.PATIENT_ID, patientId);
                exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);

                exchange.setProperty(CamelConstants.IDENTIFIER_OBJECT, new TokenParam(system, value));
                exchange.getIn().setHeader(CamelConstants.IDENTIFIER_STRING,value);
                exchange.getIn().setHeader(CamelConstants.PATIENT_ID_TYPE, "IDENTIFIER");
            }   
        }
    }

    public  JsonNode getPatientInfoResource(String resourceJson) {
        //PatientReferenceProcessor
        try {
            // Parse the JSON
            JsonNode rootNode = objectMapper.readTree(resourceJson);

            // Check if the resource is a Bundle
            if (rootNode.has("resourceType") && "Bundle".equals(rootNode.get("resourceType").asText())) {
                // Handle as a Bundle
                JsonNode entryArray = rootNode.path("entry");
                for (JsonNode entryNode : entryArray) {
                    // Get the resource in each entry
                    JsonNode resourceNode = entryNode.path("resource");
                    return extractPatientFromResource(resourceNode);
                }
            } else {
                // Handle as a single resource
                return extractPatientFromResource(rootNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Return null if no patient ID is found
    }

   
    public  JsonNode extractPatientFromResource(JsonNode resourceNode) {
        // If the resource itself is a Patient, return its ID
        if (resourceNode.has("resourceType") && "Patient".equals(resourceNode.get("resourceType").asText())) {
            return resourceNode;
        }

        JsonNode referenceNode;
        String resourceType = resourceNode.path("resourceType").asText();
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
            throw new UnprocessableEntityException(resourceNode.path("resourceType") + " should be linked to a subject/patient");
        }

    }

    public  String extractPatientIdFromReference(Exchange exchange, JsonNode resourceNode, String reference) {
        if (reference != null) {
            if (reference.startsWith("Patient/")) {
                //Internal Reference
                // Relative reference to Patient resource
                exchange.getIn().setHeader(CamelConstants.PATIENT_ID_TYPE, "RELATIVE_REFERENCE");
                return reference.split("/")[1];
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

                exchange.getIn().setHeader(CamelConstants.PATIENT_ID_TYPE, "ABSOLUTE_REFERENCE");
                String referenceStr = reference.split("#")[0];
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
                exchange.getIn().setHeader(CamelConstants.PATIENT_ID_TYPE, "ABSOLUTE_REFERENCE");
                JsonNode resource = extractFullUrlResource(resourceNode, reference);
                if (resource != null) {
                    return resource.get("id").asText();
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
                exchange.getIn().setHeader(CamelConstants.PATIENT_ID_TYPE, "EXTERNAL_REFERENCE");
                return reference;
            }
        }
        return null;
    }

    public  String getServerPatientIdFromDb(Exchange exchange, String patientId) {

        //get the internalPatientId from db
        String internalPatientId = Optional.ofNullable(patientEhrRepository.findByInputPatientId(patientId))  
                                    .map(PatientEhr::getInternalPatientId)  
                                    .orElseGet(() -> Optional.ofNullable(patientEhrRepository.findByInternalPatientId(patientId))
                                    .map(PatientEhr::getInternalPatientId)  
                                    .orElse(null));  
        
        return internalPatientId;
    }


    public  String getPatientIdFromOutCome(Exchange exchange) {
        MethodOutcome resource = (MethodOutcome) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);

        //Ned to get the Patient ID from the Outcome
        // String inputResourceId = resource.getId().getResourceType() + "/" + resource.getId().getValue();
        String serverPatientId = resource.getId().getIdPart();

        exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
        exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_RESOURCE, (Patient)resource.getResource());

        return serverPatientId;
    }

    public  String getPatientIdFromResponse(Exchange exchange) {
        String responseString = (String) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);

        JsonNode resource = null;
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(responseString);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Extract the location if it starts with "Patient/"
        Optional<String> patientLocation = Optional.ofNullable(rootNode.get("entry"))
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
                            .filter(location -> location != null)
                            .findFirst(); 
                })
                .orElse(Optional.empty());
                
            if (patientLocation.isPresent()) {
                String  serverPatientId = patientLocation.get();
                exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
                return serverPatientId;
            } else {
                return null;
            }
    }

    public static Identifier getPseudonym(Patient patient) {
        return patient.getIdentifier().stream()
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Patient must have an identifier"));
    }

    private  JsonNode extractFullUrlResource(JsonNode rootNode, String fullUrl) {
        JsonNode resource = null;

        // Directly target the `entry` array
        if (rootNode.has("entry") && rootNode.get("entry").isArray()) {
            for (JsonNode entry : rootNode.get("entry")) {
                if (entry.has("fullUrl") && entry.get("fullUrl").isTextual() && fullUrl.equals(entry.get("fullUrl").asText())) {
                    resource = entry.deepCopy();
                }
            }
        }
        return resource;
    }

    private  JsonNode extractContainedResource(JsonNode rootNode, String containedId) {
        JsonNode resource = null;

        // Directly target the `contained` array
        if (rootNode.has("contained") && rootNode.get("contained").isArray()) {
            for (JsonNode contained : rootNode.get("contained")) {
                if (contained.has("resourceType") 
                && contained.get("resourceType").isTextual() 
                && "Patient".equals(contained.get("resourceType").asText())
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
