package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

@Component
public class PatientUtils {
    private static final Logger LOG = LoggerFactory.getLogger(PatientUtils.class);
    
    private PatientEhrRepository patientEhrRepository;

    public PatientUtils(PatientEhrRepository patientEhrRepository) {
        this.patientEhrRepository = patientEhrRepository;
    }

    @Handler
    public void extractPatientIdOrIdentifier(Exchange exchange) {
        Resource resource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
        String patientId = null;
        String serverPatientId = null;

        if (resource instanceof Patient) {
            // Handle Patient resource
            Patient patient = (Patient) resource;
            Identifier identifier = patient.getIdentifier().stream()
                    .findFirst()
                    .orElse(null);

            if (identifier != null) {
                patientId = identifier.getSystem() + "|" + identifier.getValue();
                serverPatientId = getServerPatientIdFromDb(patientId);
                
                if (serverPatientId != null) {
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
            String reference = subject.getReference();
            if (subject.hasReference()) {
                patientId = handlePatientReference(exchange, resource, reference);
                serverPatientId = getServerPatientIdFromDb(patientId);

                exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
            } else if (hasIdentifier(subject)) {
                // Handle identifier-based reference
                Identifier identifier = subject.getIdentifier();
                if (identifier != null) {
                    patientId = identifier.getSystem() + "|" + identifier.getValue();
                    serverPatientId = getServerPatientIdFromDb(patientId);

                    exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, patientId);
                    exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
                    exchange.setProperty(CamelConstants.IDENTIFIER_OBJECT, new TokenParam(identifier.getSystem(), identifier.getValue()));
                    exchange.getIn().setHeader(CamelConstants.IDENTIFIER_STRING, identifier.getValue());
                    exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "IDENTIFIER");
                }
            } else {
                throw new UnprocessableEntityException("Subject identifier is required");
            }
        }
    }

    private boolean hasIdentifier(Reference subject) {
        return subject.hasIdentifier() &&
                subject.getIdentifier().hasSystem() &&
                subject.getIdentifier().hasValue();
    }

    private Reference extractPatientReference(Resource resource) {
        if (resource instanceof Bundle) {
            Bundle bundle = (Bundle) resource;
            return bundle.getEntry().stream()
                .map(entry -> {
                    try {
                        if (Resources.isPatient(entry.getResource())) {
                            return null;
                        } else {
                            return Resources.getSubject(entry.getResource())
                                    .orElse(null);
                        }
                    } catch (UnprocessableEntityException e) {
                        // Continue searching if this entry doesn't have a subject
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnprocessableEntityException("Bundle does not contain any resources with patient references"));
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
            if (resource instanceof Bundle) {
                Bundle bundle = (Bundle) resource;
                Optional<Patient> patient = findPatientInBundle(bundle, reference);
                return patient.map(p -> p.getId()).orElse(null);
            }
            return null;
        } else {
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "EXTERNAL_REFERENCE");
            return reference;
        }
    }

    private Patient findContainedPatient(Resource resource, String containedId) {
        if (resource instanceof DomainResource) {
            return ((DomainResource) resource).getContained().stream()
                    .filter(r -> r instanceof Patient && containedId.equals(r.getId()))
                    .map(r -> (Patient) r)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private Optional<Patient> findPatientInBundle(Bundle bundle, String fullUrl) {
        return bundle.getEntry().stream()
                .filter(entry -> fullUrl.equals(entry.getFullUrl()))
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Patient)
                .map(r -> (Patient) r)
                .findFirst();
    }

    public String getServerPatientIdFromDb(String patientId) {
        return Optional.ofNullable(patientEhrRepository.findByInputPatientId(patientId))  
                .map(PatientEhr::getInternalPatientId)  
                .orElseGet(() -> Optional.ofNullable(patientEhrRepository.findByInternalPatientId(patientId))
                .map(PatientEhr::getInternalPatientId)  
                .orElse(null));  
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
        Patient patient = (Patient) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
        String patientId = null;
        
        // Get the first identifier
        Optional<Identifier> firstIdentifier = patient.getIdentifier().stream().findFirst();
        if (firstIdentifier.isPresent()) {
            String system = firstIdentifier.get().getSystem();
            String value = firstIdentifier.get().getValue();
            patientId = system + "|" + value;
        }

        String serverPatientId = getServerPatientIdFromDb(patientId);
        if (serverPatientId != null) {
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

            patientLocation.ifPresent(location -> {
                exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, location);
            });
        }
    }

    public static Identifier getPseudonym(Patient patient) {
        return patient.getIdentifier().stream()
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Patient must have an identifier"));
    }
}
