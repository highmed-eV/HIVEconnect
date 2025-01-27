package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.regex.Pattern;

public class FhirUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

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

    public static @NotNull List<String> getResourceIds(String resourceJson) {
        try {
            // Parse the JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(resourceJson);
            List<String> extractedResourceIds = new ArrayList<>();
            Set<String> resultSet = new HashSet<>();

            List<String> fullUrlList = extractFullUrls(rootNode);
            // Check if the resource is a Bundle
            if (rootNode.has("resourceType") && "Bundle".equals(rootNode.get("resourceType").asText())) {
                // Handle as a Bundle
                // Collect all resource IDs from "reference" keys
                extractedResourceIds = extractResourceIds(rootNode, fullUrlList);
            } else {
                // Handle as a single resource
                extractedResourceIds = extractResourceIds(rootNode, fullUrlList);
            }
            if (!Objects.isNull(extractedResourceIds)) {
                // Collect all the uniques resource IDs
                resultSet.addAll(extractedResourceIds);
                // List of all the unique resource Ids in the input bundle or single resource
                return new ArrayList<>(resultSet);
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

    public static String serializeOperationOutcome(IBaseOperationOutcome outcome) {
        return FHIR_CONTEXT.newJsonParser().encodeResourceToString(outcome);
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


