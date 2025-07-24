package org.highmed.hiveconnect.fhir.support;

import org.hl7.fhir.r4.model.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("java:S6212")
public class Resources {

    private static final String UNSUPPORTED_RESOURCE_TYPE = "Unsupported resource type: ";

    private Resources() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isPatient(Resource resource) {
        return resource != null && resource.getResourceType() == ResourceType.Patient;
    }

    public static boolean isReferenceType(Reference reference, ResourceType resourceType) {
        if (reference == null || resourceType == null) {
            return false;
        }
        return reference.hasType() && reference.getType().equals(resourceType.name());
    }

    public static Optional<Reference> getSubject(Resource resource) {
        return switch (resource.getResourceType()) {
            case Condition -> getSubject((Condition) resource);
            case Consent -> getPatient((Consent) resource);
            case DiagnosticReport -> getSubject((DiagnosticReport) resource);
            case DocumentReference -> getSubject((DocumentReference) resource);
            case Encounter -> getSubject((Encounter) resource);
            case Immunization -> getPatient((Immunization) resource);
            case MedicationStatement -> getSubject((MedicationStatement) resource);
            case MedicationRequest -> getSubject((MedicationRequest) resource);
            case Observation -> getSubject((Observation) resource);
            case Procedure -> getSubject((Procedure) resource);
            case QuestionnaireResponse -> getSubject((QuestionnaireResponse) resource);
            case Specimen -> getSubject((Specimen) resource);
            case Composition -> getSubject((Composition) resource);
            case ResearchSubject -> getIndividual((ResearchSubject) resource);
            case ServiceRequest -> getSubject((ServiceRequest) resource);
            case MedicationAdministration -> getSubject((MedicationAdministration) resource);
            case Medication -> getSubject((Medication) resource);
            case Organization -> getSubject((Organization) resource);
            case List -> getSubject((ListResource) resource);
            default -> throw new IllegalArgumentException( UNSUPPORTED_RESOURCE_TYPE+ resource.getResourceType());
        };
    }

    public static void setSubject(Resource resource, Reference subject) {
        
        switch (resource.getResourceType()) {
            case Condition:
                ((Condition) resource).setSubject(subject);
                break;
            case Consent:
                ((Consent) resource).setPatient(subject);
                break;
            case DiagnosticReport:
                ((DiagnosticReport) resource).setSubject(subject);
                break;
            case DocumentReference:
                ((DocumentReference) resource).setSubject(subject);
                break;
            case Encounter:
                ((Encounter) resource).setSubject(subject);
                break;
            case Immunization:
                ((Immunization) resource).setPatient(subject);
                break;
            case MedicationStatement:
                ((MedicationStatement) resource).setSubject(subject);
                break;
            case MedicationRequest:
                ((MedicationRequest) resource).setSubject(subject);
                break;
            case Observation:
                ((Observation) resource).setSubject(subject);
                break;
            case Procedure:
                ((Procedure) resource).setSubject(subject);
                break;
            case QuestionnaireResponse:
                ((QuestionnaireResponse) resource).setSubject(subject);
                break;
            case Specimen:
                ((Specimen) resource).setSubject(subject);
                break;
            case Composition:
                ((Composition) resource).setSubject(subject);
                break;
            case ResearchSubject:
                ((ResearchSubject) resource).setIndividual(subject);
                break;
            case ServiceRequest:
                ((ServiceRequest) resource).setSubject(subject);
                break;
            case MedicationAdministration:
                ((MedicationAdministration) resource).setSubject(subject);
                break;
            case List:
                ((ListResource) resource).setSubject(subject);
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_RESOURCE_TYPE + resource.getResourceType());
        }
    }

    //A more genric approach to get the subject from the resource
    public static Reference getSubjectGeneric(Resource resource) {
            // Handle Patient resource explicitly
            if (resource instanceof Patient) {
                return new Reference(resource.getIdElement().getValue());
            }
        
            // List of common fields that may contain patient references
            String[] patientReferenceFields = {"subject", "patient", "individual"};
        
            // Use reflection to check for common fields
            for (String field : patientReferenceFields) {
                try {
                    // Capitalize the field name to match the getter method name
                    String getterName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
        
                    // Get the getter method for the field
                    Method getter = resource.getClass().getMethod(getterName);
        
                    // Invoke the getter method
                    Object result = getter.invoke(resource);
        
                    // If the result is a Reference, return it
                    if (result instanceof Reference reference && reference.hasReference()) {
                            return reference;
                        }

                } catch (Exception e) {
                    // Ignore exceptions and continue checking other fields
                }
            }
        
            // If no patient reference is found, throw an exception
            throw new IllegalArgumentException(UNSUPPORTED_RESOURCE_TYPE + resource.getResourceType());
        }

    public static List<String> getProfileUris(Resource resource) {
        return resource.getMeta().getProfile()
                .stream()
                .map(CanonicalType::getValue)
                .toList();
    }

    private static Optional<Reference> getSubject(Condition condition) {
        return condition.hasSubject() ? Optional.of(condition.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getPatient(Consent consent) {
        return consent.hasPatient() ? Optional.of(consent.getPatient()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(DiagnosticReport diagnosticReport) {
        return diagnosticReport.hasSubject() ? Optional.of(diagnosticReport.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(DocumentReference documentReference) {
        return documentReference.hasSubject() ? Optional.of(documentReference.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(Encounter encounter) {
        return encounter.hasSubject() ? Optional.of(encounter.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getPatient(Immunization immunization) {
        return immunization.hasPatient() ? Optional.of(immunization.getPatient()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(MedicationStatement medicationStatement) {
        return medicationStatement.hasSubject() ? Optional.of(medicationStatement.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(MedicationRequest medicationRequest) {
        return medicationRequest.hasSubject() ? Optional.of(medicationRequest.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(Observation observation) {
        return observation.hasSubject() ? Optional.of(observation.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(Procedure procedure) {
        return procedure.hasSubject() ? Optional.of(procedure.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(QuestionnaireResponse questionnaireResponse) {
        return questionnaireResponse.hasSubject() ? Optional.of(questionnaireResponse.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(Specimen specimen) {
        return specimen.hasSubject() ? Optional.of(specimen.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(Composition composition) {
        return composition.hasSubject() ? Optional.of(composition.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getIndividual(ResearchSubject researchSubject) {
        return researchSubject.hasIndividual() ? Optional.of(researchSubject.getIndividual()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(ServiceRequest serviceRequest) {
        return serviceRequest.hasSubject() ? Optional.of(serviceRequest.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(MedicationAdministration medicationAdministration) {
        return medicationAdministration.hasSubject() ? Optional.of(medicationAdministration.getSubject()) : Optional.empty();
    }

    private static Optional<Reference> getSubject(Medication medication) {
        return  Optional.empty();
    }

    private static Optional<Reference> getSubject(Organization organization) {
        return Optional.empty();
    }    
    private static Optional<Reference> getSubject(ListResource listResource) {
        return listResource.hasSubject() ? Optional.of(listResource.getSubject()) : Optional.empty();
    }

}
