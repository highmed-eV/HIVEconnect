package org.ehrbase.fhirbridge.fhir.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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

    public static String getPatientId(String resourceJson) {
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
                    String patientId = extractPatientIdFromResource(resourceNode);
                    if (patientId != null) {
                        return patientId;
                    }
                }
            } else {
                // Handle as a single resource
                return extractPatientIdFromResource(rootNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Return null if no patient ID is found
    }

    public static String getPatientIdFromOutCome(MethodOutcome resource){
        //Ned to get the Patient ID from the Outcome
        // String inputResourceId = resource.getId().getResourceType() + "/" + resource.getId().getValue();
        String inputResourceId = resource.getId().getIdPart();
        return inputResourceId;
    }

    private static String extractPatientIdFromResource(JsonNode resourceNode) {
        // If the resource itself is a Patient, return its ID
        if (resourceNode.has("resourceType") && "Patient".equals(resourceNode.get("resourceType").asText())) {
            return resourceNode.path("id").asText();
        }

        // Look for the "subject" field within the resource
        JsonNode referenceNode = resourceNode.path("subject");
        //patient(Consent, Immunization) individual(ResearchSubject)

        if (!referenceNode.isMissingNode()) {
            // Check for "reference" field in subject
            if (referenceNode.has("reference")) {
                String reference = referenceNode.get("reference").asText();
                return extractPatientIdFromReference(reference);
            } else if (referenceNode.has("identifier")) {
                // Check for "identifier" field in subject
                // private IIdType handleSubjectReferenceInternal(Reference subject, RequestDetails requestDetails) {
                //     Patient patientReference = (Patient) subject.getResource();
                //     Identifier identifier =  patientReference.getIdentifier().get(0); //TODO bad practice
                //     SearchParameterMap parameters = new SearchParameterMap();
                //     parameters.add(Patient.SP_IDENTIFIER, new TokenParam(identifier.getSystem(), identifier.getValue()));
                //     Set<ResourcePersistentId> ids = patientDao.searchForIds(parameters, requestDetails);
                //     IIdType patientId;
                //     if (ids.isEmpty()) {
                //         patientId = createPatient(identifier, requestDetails);
                //     } else if (ids.size() == 1) {
                //         IBundleProvider bundleProvider = patientDao.search(parameters, requestDetails);
                //         List<IBaseResource> result = bundleProvider.getResources(0, 1);
                //         Patient patient = (Patient) result.get(0);
                //         patientId = patient.getIdElement();
                //         LOG.debug("Resolved existing Patient: id={}", patientId);
                //     } else {
                //         throw new UnprocessableEntityException("More than one patient matching the given identifier system and value");
                //     }
                //     subject.setReferenceElement(patientId);
                //     return patientId;
                // }

                //handleSubjectReferenceInternal
                //TODO: new TokenParam(identifier.getSystem(), identifier.getValue())
                JsonNode identifierNode = referenceNode.get("identifier");
                if (identifierNode.has("value")) {
                    return identifierNode.get("value").asText();
                }
            } else {
                throw new UnprocessableEntityException("Subject identifier is required");
            }
        } else {
            throw new UnprocessableEntityException(resourceNode.path("resourceType") + " should be linked to a subject/patient");
        }

        return null;
    }

    private static String extractPatientIdFromReference(String reference) {
        if (reference != null) {
            if (reference.startsWith("Patient/")) {
                //Internal Reference
                // Relative reference to Patient resource
                return reference.split("/")[1];
            } else if (reference.startsWith("#")) {
                // Internal contained reference
                //TODO Check what needs to be done here
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

                // return reference.substring(1);

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

                // return reference.substring(1);
            } else {
                // External reference (absolute URL)
                //TODO Check what needs to be done here
                // External/absolute url:
                // {
                // "subject": {
                //     "reference": "http://external-fhir-server.com/Patient/456"
                // }
                // }

                //create ehrid for this "http://external-fhir-server.com/Patient/456
                //maintain mapping between the patientid, ehrid record.
                //corresponding compositionid

                //taking external ref id to internalid(dummy patient).
                //external id to dummy patient
                


                // if (reference.contains("Patient")) {
                //     return reference.substring(reference.lastIndexOf("/") + 1);
                // }
            }
        }
        return null;
    }

    public static List<String> getResourceIds(String resourceJson) {
        //ResourceReferenceProcessor
        try {
            // Parse the JSON
            JsonNode rootNode = objectMapper.readTree(resourceJson);
            Set<String> resultSet = new HashSet<>();
            // Check if the resource is a Bundle
            if (rootNode.has("resourceType") && "Bundle".equals(rootNode.get("resourceType").asText())) {
                // Handle as a Bundle
                JsonNode entryArray = rootNode.path("entry");
                for (JsonNode entryNode : entryArray) {
                    List<String> extractedResourceIds = new ArrayList<>();
                    // Get the resource in each entry
                    JsonNode resourceNode = entryNode.path("resource");
                    extractedResourceIds = extractResourceIdFromResource(resourceNode);
                    resultSet.addAll(extractedResourceIds);
                }
                // List of all the resource Ids in the input bundle or single resource
                List<String> resourceIds = new ArrayList<>(resultSet);
                if (resourceIds != null) {
                    return resourceIds;
                }
            } else {
                // Handle as a single resource
                return extractResourceIdFromResource(rootNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Return null if no resource ID is found
    }

    private static List<String> extractResourceIdFromResource(JsonNode resourceNode) {

        // List of all the resource Ids in the input bundle or single resource
        List<String> resourceIds = new ArrayList<>();

        // If the resource itself is a Present, return its ID
        if (resourceNode.has("resourceType")) {
            String resourceId = resourceNode.get("resourceType").asText() + "/" + resourceNode.get("id").asText();
            resourceIds.add(resourceId);
        }

        // Look for the "subject" field within the resource to get the resourceId
        if (resourceNode.has("subject")) {
            JsonNode referenceNode = resourceNode.path("subject");

            if (!referenceNode.isMissingNode()) {
                // Check for "reference" field in subject
                if (referenceNode.has("reference")) {
                    String reference = referenceNode.get("reference").asText();
                    String resourceId = extractResourceIdFromReference(reference);
                    resourceIds.add(resourceId);
                } else if (referenceNode.has("identifier")) {
                    // Check for "identifier" field in subject

                    //handleSubjectReferenceInternal
                    //TODO: new TokenParam(identifier.getSystem(), identifier.getValue())
                    JsonNode identifierNode = referenceNode.get("identifier");
                    if (identifierNode.has("value")) {
                        String resourceId = resourceNode.get("resourceType").asText() + "/" + identifierNode.get("value").asText();
                        resourceIds.add(resourceId);
                    }
                } else {
                    throw new UnprocessableEntityException("Subject identifier is required");
                }
            } else {
                throw new UnprocessableEntityException(resourceNode.path("resourceType") + " should be linked to a subject");
            }
        }

        return resourceIds;
    }

    private static String extractResourceIdFromReference(String reference) {
        if (reference != null) {
            String regex = "^[^/]+/[A-Za-z0-9-]+$";
            if (Pattern.matches(regex, reference)) {
                // Internal
                // Relative reference to resource
                return reference;
            } else if (reference.startsWith("#")) {
                // Internal contained reference
                //TODO Check what needs to be done here
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

                // return reference.substring(1);

            } else if (reference.startsWith("urn:uuid")) {
                // Internal reference

            } else {
                // External reference (absolute URL)
                //TODO Check what needs to be done here
                // External/absolute url:
                // {
                // "subject": {
                //     "reference": "http://external-fhir-server.com/Patient/456"
                // }
                // }
            }
        }
        return null;
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


