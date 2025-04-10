package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class PatientUtils {
    
    private final PatientEhrRepository patientEhrRepository;

    public PatientUtils(PatientEhrRepository patientEhrRepository) {
        this.patientEhrRepository = patientEhrRepository;
    }

    @Handler
    public void extractPatientIdOrIdentifier(Exchange exchange) {
        String systemId = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID);
        Resource resource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
        String patientId = null;
        String serverPatientId = null;
        UUID ehrId = null;

        if (resource instanceof Patient patient) {
            // Handle Patient resource
            Identifier identifier = patient.getIdentifier().stream()
                    .findFirst()
                    .orElse(null);

            if (identifier != null) {
                patientId = identifier.getSystem() + "|" + identifier.getValue();
                PatientEhr serverPatient = getServerPatientIdFromDb(patientId, systemId);
                if (serverPatient != null) {
                    serverPatientId = serverPatient.getInternalPatientId();
                    throw new UnprocessableEntityException("Patient: " + patientId + " absolute reference already exists. Please provide relative reference: " + serverPatientId);
                }
            }
            
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "ABSOLUTE_PATIENT");
            return;
        }

        // Handle other resources with patient references
        Reference subject = extractPatientReference(resource);
        if (subject != null) {
            handleSubject(exchange, systemId, resource, subject);
        }
    }
    private void handleSubject(Exchange exchange, String systemId, Resource resource, Reference subject) {
        String patientId = null;
        String serverPatientId = null;
        UUID ehrId = null;

        String reference = subject.getReference();
        if (subject.hasReference()) {
            patientId = handlePatientReference(exchange, resource, reference);
            PatientEhr serverPatient = getServerPatientIdFromDb(patientId, systemId);
            if (serverPatient != null) {
                serverPatientId = serverPatient.getInternalPatientId();
                ehrId = serverPatient.getEhrId();
            }

            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
            exchange.getIn().setHeader(CompositionConstants.EHR_ID, ehrId);
        } else if (hasIdentifier(subject)) {
            // Handle identifier-based reference
            Identifier identifier = subject.getIdentifier();
            if (identifier != null) {
                patientId = identifier.getSystem() + "|" + identifier.getValue();
                PatientEhr serverPatient = getServerPatientIdFromDb(patientId, systemId);
                if (serverPatient != null) {
                    serverPatientId = serverPatient.getInternalPatientId();
                    ehrId = serverPatient.getEhrId();
                }

                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
                exchange.setProperty(CamelConstants.IDENTIFIER_OBJECT, new TokenParam(identifier.getSystem(), identifier.getValue()));
                exchange.getIn().setHeader(CamelConstants.IDENTIFIER_STRING, identifier.getValue());
                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "IDENTIFIER");
                exchange.getIn().setHeader(CompositionConstants.EHR_ID, ehrId);
            }
        } else {
            throw new UnprocessableEntityException("Subject identifier is required");
        }
    }

    private boolean hasIdentifier(Reference subject) {
        return subject.hasIdentifier() &&
                subject.getIdentifier().hasSystem() &&
                subject.getIdentifier().hasValue();
    }

    private Reference extractPatientReference(Resource resource) {
        if (resource instanceof Bundle bundle) {
            //Supporting homogenous patient references in bundle for now.
            //Not supporting mix of  Relative reference with serch urls although this is possible
            //In order to support this we need to first resolve the serch url to relative reference
            //eg: Patient/123 and Patient?identifier=12345
            //First resolve the serch url by calling server
            //get the Relative reference for this and check if it is present in the bundle  
            //if not then throw error
            //if yes then return the relative reference
            //TODO: Support heterogenous patient references in bundle.
            List<Reference> subjectReferences = bundle.getEntry().stream()
            .map(entry -> {
                try {
                    if (Resources.isPatient(entry.getResource())) {
                        return null; // Skip Patient resources
                    } else {
                        return Resources.getSubject(entry.getResource())
                                .orElse(null); // Get subject reference, if any
                    }
                } catch (UnprocessableEntityException e) {
                    // Skip entries that don't have a subject
                    return null;
                }
            })
            .filter(Objects::nonNull) // Filter out null values
            .toList(); // Collect all subject references into a list

            // Check if the bundle contains any resources with subject references
            if (subjectReferences.isEmpty()) {
                throw new UnprocessableEntityException("Bundle does not contain any resources with patient references");
            }

            Reference firstReference = subjectReferences.get(0);
            boolean referencesMatch;
            
            // Check if first reference is an identifier-based reference
            if (firstReference.hasIdentifier()) {
                // All should be identifier references with matching system and value
                referencesMatch = subjectReferences.stream()
                        .allMatch(ref -> ref.hasIdentifier() 
                                && ref.getIdentifier().getSystem().equals(firstReference.getIdentifier().getSystem())
                                && ref.getIdentifier().getValue().equals(firstReference.getIdentifier().getValue()));
            } else if (firstReference.hasReference()) {
                // All should be string references with matching values
                referencesMatch = subjectReferences.stream()
                        .allMatch(ref -> ref.hasReference() 
                                && ref.getReference().equals(firstReference.getReference()));
            } else {
                referencesMatch = false;
            }

            if (!referencesMatch) {
                throw new UnprocessableEntityException("Bundle contains resources with inconsistent patient references");
            }

            return firstReference;
        }
        
        return Resources.getSubject(resource)
                .orElseThrow(() -> new UnprocessableEntityException(resource.getResourceType().name() + " should be linked to a subject/patient"));
    }

    private String handlePatientReference(Exchange exchange, Resource resource, String reference) {
        if (reference.startsWith("Patient/")) {
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "RELATIVE_REFERENCE");
            return reference;
        } else if (reference.startsWith("#")) {
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "ABSOLUTE_REFERENCE");
            String containedId = reference.substring(1);
            Patient containedPatient = findContainedPatient(resource, containedId);
            return containedPatient != null ? containedId : null;
        } else if (reference.startsWith("urn:uuid:")) {
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "ABSOLUTE_REFERENCE");
            if (resource instanceof Bundle bundle) {
                Optional<Patient> patient = findPatientInBundle(bundle, reference);
                return patient.map(Resource::getId).orElse(null);
            }
            return null;
        } else if (reference.startsWith("Patient?")) {
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "SEARCH_URL");
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_SEARCH_URL, reference);
            return reference;
        }  else {
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "EXTERNAL_REFERENCE");
            return reference;
        }
    }

    private Patient findContainedPatient(Resource resource, String containedId) {
        return ((DomainResource) resource).getContained().stream()
                .filter(r -> r instanceof Patient && containedId.equals(r.getId()))
                .map(r -> (Patient) r)
                .findFirst()
                .orElse(null);

    }

    private Optional<Patient> findPatientInBundle(Bundle bundle, String fullUrl) {
        return bundle.getEntry().stream()
                .filter(entry -> fullUrl.equals(entry.getFullUrl()))
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Patient.class::isInstance)
                .map(r -> (Patient) r)
                .findFirst();
    }

    public PatientEhr getServerPatientIdFromDb(String patientId, String systemId) {
        return Optional.ofNullable(patientEhrRepository.findByInputPatientIdAndSystemId(patientId, systemId))  
                .orElseGet(() -> patientEhrRepository.findByInternalPatientIdAndSystemId(patientId, systemId));
    }

    public void getPatientIdAndResourceIdFromOutCome(Exchange exchange) {
        String serverPatientId = (String) exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID);
        MethodOutcome methodOutcome = (MethodOutcome) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
        String resourceType = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE);
        
        //Get the Resource ID from the Outcome
        String serverResourceId = methodOutcome.getId().getResourceType() + "/" + methodOutcome.getId().getIdPart();
        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID, serverResourceId);

        //get patient id if not present
        if (serverPatientId == null) {
            if ("Patient".equals(resourceType)) {
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverResourceId);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE, methodOutcome.getResource());
            } else {
                // get the subject reference 
                serverPatientId = (String) exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
            }
        }
    }

    public void getPatientIdFromPatientResource(Exchange exchange) {
        String systemId = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID);
        Patient patient = (Patient) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
        String patientId = null;
        
        // Get the first identifier
        Optional<Identifier> firstIdentifier = patient.getIdentifier().stream().findFirst();
        if (firstIdentifier.isPresent()) {
            String system = firstIdentifier.get().getSystem();
            String value = firstIdentifier.get().getValue();
            patientId = system + "|" + value;
        }

        PatientEhr serverPatient = getServerPatientIdFromDb(patientId, systemId);
        if (serverPatient != null) {
            //This can be the case when Patient resource is part of the bundle and the patient is already created
            throw new UnprocessableEntityException("Patient: " + patientId + " already exists");
        }
        exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
    }

    public void getPatientIdAndResourceIdFromResponse(Exchange exchange) {
        String serverPatientId = (String) exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID);
        Bundle responseBundle = (Bundle) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);

        if (responseBundle != null && !responseBundle.getEntry().isEmpty()) {
            // Get the first resource ID from the response
            Bundle.BundleEntryComponent firstEntry = responseBundle.getEntryFirstRep();
            if (firstEntry.hasResponse() && firstEntry.getResponse().hasLocation()) {
                String location = firstEntry.getResponse().getLocation();
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID, location);
            }

            if (serverPatientId != null) {
                // Patient id already found via reference id, so return
                return;
            }

            // Find Patient resource location in the bundle
            Optional<String> patientLocation = responseBundle.getEntry().stream()
                .filter(entry -> entry.hasResponse() && entry.getResponse().hasLocation())
                .map(entry -> entry.getResponse().getLocation())
                .filter(location -> location != null && location.startsWith("Patient/"))
                .map(location -> {
                    // Truncate the location before "/_history" if present
                    int historyIndex = location.indexOf("/_history");
                    return historyIndex != -1 ? location.substring(0, historyIndex) : location;
                })
                .findFirst();

            patientLocation.ifPresent(location -> exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, location));
        }
    }

    public static Identifier getPseudonym(Patient patient) {
        return patient.getIdentifier().stream()
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Patient must have an identifier"));
    }
}
