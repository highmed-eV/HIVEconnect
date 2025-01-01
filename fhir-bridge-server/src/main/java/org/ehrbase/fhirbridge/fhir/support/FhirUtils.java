package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class FhirUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getResourceType(String resourceJson) {
        try {
            // Parse the JSON
            JsonNode rootNode = objectMapper.readTree(resourceJson);

            // Get resourceType
            if (rootNode.has("resourceType")) {
                return rootNode.get("resourceType").asText();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Return null if no patient ID is found
    }

    public static JsonNode getPatientInfoResource(String resourceJson) {
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

    public static String getPatientIdFromOutCome(MethodOutcome resource) {
        //Ned to get the Patient ID from the Outcome
        // String inputResourceId = resource.getId().getResourceType() + "/" + resource.getId().getValue();
        String inputResourceId = resource.getId().getIdPart();
        return inputResourceId;
    }

    public static String getPatientIdFromResponse(String responseString) {
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
                
            if ( patientLocation.isPresent() ) {
                return patientLocation.get();
            } else
            {
                return null;
            }
    }

    public static JsonNode extractPatientFromResource(JsonNode resourceNode) {
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

    public static String extractPatientIdFromReference(JsonNode resourceNode, String reference) {
        if (reference != null) {
            if (reference.startsWith("Patient/")) {
                //Internal Reference
                // Relative reference to Patient resource
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
                return reference;
            }
        }
        return null;
    }

    public static @NotNull List<String> getResourceIds(String resourceJson) {
        try {
            // Parse the JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(resourceJson);
            Set<String> resultSet = new HashSet<>();

            List<String> fullUrlList = extractFullUrls(rootNode);
            // Check if the resource is a Bundle
            if (rootNode.has("resourceType") && "Bundle".equals(rootNode.get("resourceType").asText())) {
                // Handle as a Bundle
                // Collect all resource IDs from "reference" keys
                List<String> extractedResourceIds = extractResourceIds(rootNode, fullUrlList);
                if (!Objects.isNull(extractedResourceIds)) {
                    // Collect all the uniques resource IDs
                    resultSet.addAll(extractedResourceIds);
                    // List of all the unique resource Ids in the input bundle or single resource
                    return new ArrayList<>(resultSet);
                }
            } else {
                // Handle as a single resource
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList(); // Return an empty list if no resource ID is found
    }

    private static List<String> extractResourceIds(JsonNode resourceNode, List<String> fullUrlList) {
        List<String> resourceIds = new ArrayList<>();
        String regex = "^[^/]+/[A-Za-z0-9-]+$";

        if (resourceNode.isObject()) {
            Iterator<String> fieldNames = resourceNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode childNode = resourceNode.get(fieldName);
                // Skip the "subject" field
                if ("subject".equals(fieldName)) {
                    continue;
                }
                // If "reference" key is found, extract the ID
                if ("reference".equals(fieldName) && childNode.isTextual()) {
                    String reference = childNode.asText();
                    if (Pattern.matches(regex, reference) || (reference.startsWith("urn:uuid") && !fullUrlList.contains(reference))) {
                        // Extract resource ID from the reference value
                        resourceIds.add(reference);
                        break;
                    }
                } else {
                    // Recursively process child nodes
                    resourceIds.addAll(extractResourceIds(childNode, fullUrlList));
                }
            }
        } else if (resourceNode.isArray()) {
            for (JsonNode arrayElement : resourceNode) {
                resourceIds.addAll(extractResourceIds(arrayElement, fullUrlList));
            }
        }
        return resourceIds;
    }

    private static List<String> extractFullUrls(JsonNode rootNode) {
        List<String> fullUrls = new ArrayList<>();

        // Directly target the `entry` array
        if (rootNode.has("entry") && rootNode.get("entry").isArray()) {
            for (JsonNode entry : rootNode.get("entry")) {
                if (entry.has("fullUrl") && entry.get("fullUrl").isTextual()) {
                    fullUrls.add(entry.get("fullUrl").asText());
                }
            }
        }
        return fullUrls;
    }

    private static JsonNode extractFullUrlResource(JsonNode rootNode, String fullUrl) {
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

    private static JsonNode extractContainedResource(JsonNode rootNode, String containedId) {
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

//Old FB
// import org.hl7.fhir.instance.model.api.IBaseResource;
// import org.hl7.fhir.r4.model.Identifier;
// import org.hl7.fhir.r4.model.Reference;
// import org.hl7.fhir.r4.model.Resource;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.hl7.fhir.r4.model.Observation;
// import org.hl7.fhir.r4.model.Encounter;
// import org.hl7.fhir.r4.model.Condition;
// import org.hl7.fhir.r4.model.MedicationRequest;
// import org.hl7.fhir.r4.model.DiagnosticReport;
// import org.hl7.fhir.r4.model.Patient;

// public class FhirUtils {
//     private static final Logger logger = LoggerFactory.getLogger(FhirUtils.class);

//     public static String getPatientId(IBaseResource resource) {
//         if (resource instanceof Resource) {
//             Resource fhirResource = (Resource) resource;

//             // Check for resources that have a subject field referencing a patient
//             Reference subjectReference = null;
//             if (fhirResource instanceof Observation) {
//                 subjectReference = ((Observation) fhirResource).getSubject();
//             } else if (fhirResource instanceof Encounter) {
//                 subjectReference = ((Encounter) fhirResource).getSubject();
//             } else if (fhirResource instanceof Condition) {
//                 subjectReference = ((Condition) fhirResource).getSubject();
//             } else if (fhirResource instanceof MedicationRequest) {
//                 subjectReference = ((MedicationRequest) fhirResource).getSubject();
//             } else if (fhirResource instanceof DiagnosticReport) {
//                 subjectReference = ((DiagnosticReport) fhirResource).getSubject();
//             }

//             // Try to get patient ID from reference if subjectReference is found
//             if (subjectReference != null) {
//                 String patientId = extractPatientIdFromReference(subjectReference);
//                 if (patientId != null) {
//                     logger.info("FHIR patient ID: {}", patientId);
//                     return patientId;
//                 }

//                 // Check for patient ID in identifier if reference is not used
//                 if (subjectReference.hasIdentifier()) {
//                     Identifier patientIdentifier = subjectReference.getIdentifier();
//                     logger.info("FHIR patient ID: {}", patientIdentifier.getValue());
//                     return patientIdentifier.getValue();
//                 }
//             }

//             // If the resource is a Patient resource, return the ID directly
//             if (fhirResource instanceof Patient) {
//                 return fhirResource.getIdElement().getIdPart();
//             }
//         }
//         logger.info("FHIR patient ID null");

//         return null; // Return null if no patient reference or identifier is found
//     }

//     private static String extractPatientIdFromReference(Reference reference) {
//         if (reference != null) {
//             String referenceValue = reference.getReference();

//             // Check for internal bundle reference (relative reference)
//             if (referenceValue != null) {
//                 if (referenceValue.startsWith("Patient/")) {
//                     // Relative reference to Patient resource
//                     return reference.getReferenceElement().getIdPart();
//                 } else if (referenceValue.startsWith("#")) {
//                     // Handle internal contained resource reference if needed
//                     // For example, use Bundle to resolve the contained resource by ID
//                     return referenceValue.substring(1); // returns the internal reference ID
//                 } else {
//                     // External reference
//                     if (reference.getReferenceElement().getResourceType().equals("Patient")) {
//                         return reference.getReferenceElement().getIdPart();
//                     }
//                 }
//             }
//         }
//         return null;
//     }
// }

}


