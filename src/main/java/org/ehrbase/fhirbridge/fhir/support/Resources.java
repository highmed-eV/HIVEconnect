package org.ehrbase.fhirbridge.fhir.support;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("java:S6212")
public class Resources {

    private static final String COVID_19_QUESTIONNAIRE_URL = "http://fhir.data4life.care/covid-19/r4/Questionnaire/covid19-recommendation";

    private Resources() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isPatient(Resource resource) {
        return resource != null && resource.getResourceType() == ResourceType.Patient;
    }

    public static boolean isQuestionnaireResponse(Resource resource) {
        return resource != null && resource.getResourceType() == ResourceType.QuestionnaireResponse;
    }

    public static boolean isCovid19Questionnaire(Resource resource) {
        return isQuestionnaireResponse(resource) &&
                StringUtils.contains(((QuestionnaireResponse) resource).getQuestionnaire(), COVID_19_QUESTIONNAIRE_URL);
    }

    public static boolean isReferenceType(Reference reference, ResourceType resourceType) {
        if (reference == null || resourceType == null) {
            return false;
        }
        return reference.hasType() && reference.getType().equals(resourceType.name());
    }

    public static Optional<Reference> getSubject(Resource resource) {
        switch (resource.getResourceType()) {
            case Condition:
                return getSubject((Condition) resource);
            case Consent:
                return getPatient((Consent) resource);
            case DiagnosticReport:
                return getSubject((DiagnosticReport) resource);
            case DocumentReference:
                return getSubject((DocumentReference) resource);
            case Encounter:
                return getSubject((Encounter) resource);
            case Immunization:
                return getPatient((Immunization) resource);
            case MedicationStatement:
                return getSubject((MedicationStatement) resource);
            case Observation:
                return getSubject((Observation) resource);
            case Procedure:
                return getSubject((Procedure) resource);
            case QuestionnaireResponse:
                return getSubject((QuestionnaireResponse) resource);
            case Specimen:
                return getSubject((Specimen) resource);
            case Composition:
                return getSubject((Composition) resource);
            case ResearchSubject:
                return getIndividual((ResearchSubject) resource);
            case ServiceRequest:
                return getSubject((ServiceRequest) resource);
            case MedicationAdministration:
                return getSubject((MedicationAdministration) resource);
            case List:
                return getSubject((ListResource) resource);
            default:
                throw new IllegalArgumentException("Unsupported resource type: " + resource.getResourceType());
        }
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
                throw new IllegalArgumentException("Unsupported resource type: " + resource.getResourceType());
        }
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

    private static Optional<Reference> getSubject(ListResource listResource) {
        return listResource.hasSubject() ? Optional.of(listResource.getSubject()) : Optional.empty();
    }

}
