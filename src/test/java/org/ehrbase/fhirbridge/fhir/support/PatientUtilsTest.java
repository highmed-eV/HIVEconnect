package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientUtilsTest {

    @Mock
    private PatientEhrRepository patientEhrRepository;

    @Mock
    private MethodOutcome methodOutcome;

    private Exchange exchange;
    private PatientUtils patientUtils;

    @BeforeEach
    void setUp() {
        patientUtils = new PatientUtils(patientEhrRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void extractPatientIdOrIdentifier_WithPatientResource() {
        // Create a Patient resource with identifier
        Patient patient = new Patient();
        Identifier identifier = new Identifier()
            .setSystem("system1")
            .setValue("value1");
        patient.addIdentifier(identifier);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, patient);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        String patientId = exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, String.class);
        assertEquals("system1|value1", patientId);
        assertEquals("ABSOLUTE_PATIENT", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientIdOrIdentifier_WithObservationResource() {
        // Create an Observation with patient reference
        Observation observation = new Observation();
        Reference patientRef = new Reference("Patient/123");
        observation.setSubject(patientRef);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, observation);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("RELATIVE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientIdOrIdentifier_WithContainedReference() {
        // Create a MedicationRequest with contained Patient
        MedicationRequest medicationRequest = new MedicationRequest();
        Patient containedPatient = new Patient();
        containedPatient.setId("patient1");
        containedPatient.addIdentifier()
            .setSystem("system1")
            .setValue("value1");
        
        medicationRequest.addContained(containedPatient);
        medicationRequest.setSubject(new Reference("#patient1"));
        

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, medicationRequest);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("patient1", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("ABSOLUTE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientIdOrIdentifier_WithBundleReference() {
        // Create a Bundle with Patient resource
        Bundle bundle = new Bundle();
        Patient patient = new Patient();
        patient.setId("patient1");
        
        Bundle.BundleEntryComponent entry = bundle.addEntry();
        entry.setFullUrl("urn:uuid:1");
        entry.setResource(patient);

        Observation obs = new Observation();
        obs.setSubject(new Reference("urn:uuid:1"));
        Bundle.BundleEntryComponent obsEntry = bundle.addEntry();
        obsEntry.setResource(obs);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, bundle);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("patient1", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("ABSOLUTE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientIdOrIdentifier_WithIdentifierReference() {
        // Create an Observation with identifier reference
        Observation observation = new Observation();
        Reference reference = new Reference();
        Identifier identifier = new Identifier()
            .setSystem("system1")
            .setValue("value1");
        reference.setIdentifier(identifier);
        observation.setSubject(reference);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, observation);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("system1|value1", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("IDENTIFIER", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
        TokenParam tokenParam = exchange.getProperty(CamelConstants.IDENTIFIER_OBJECT, TokenParam.class);
        assertNotNull(tokenParam);
        assertEquals("value1", exchange.getIn().getHeader(CamelConstants.IDENTIFIER_STRING));
    }

    @Test
    void getServerPatientIdFromDb() {
        String patientId = "system1|value1";
        PatientEhr patientEhr = mock(PatientEhr.class);
        when(patientEhrRepository.findByInputPatientId(patientId)).thenReturn(patientEhr);
        when(patientEhr.getInternalPatientId()).thenReturn("internalId");
        
        String serverPatientId = patientUtils.getServerPatientIdFromDb(patientId);
        assertEquals("internalId", serverPatientId);
    }

    @Test
    void getPatientIdAndResourceIdFromOutCome_WhenPatientResource() {
        // Create mocks
        IIdType mockIdType = mock(IIdType.class);
        Patient mockPatient = mock(Patient.class);
        
        // Setup method outcome expectations
        when(methodOutcome.getId()).thenReturn(mockIdType);
        when(methodOutcome.getResource()).thenReturn(mockPatient);
        when(mockIdType.getResourceType()).thenReturn("Patient");
        when(mockIdType.getIdPart()).thenReturn("123");

        // Setup exchange
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, methodOutcome);
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, "Patient");
        
        // Execute
        patientUtils.getPatientIdAndResourceIdFromOutCome(exchange);
        
        // Verify
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
        assertEquals(mockPatient, exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
    }

    @Test
    void getPatientIdAndResourceIdFromOutCome_WhenNonPatientResource() {
        // Create mocks
        IIdType mockIdType = mock(IIdType.class);
        
        // Setup method outcome expectations
        when(methodOutcome.getId()).thenReturn(mockIdType);
        when(mockIdType.getResourceType()).thenReturn("Observation");
        when(mockIdType.getIdPart()).thenReturn("456");

        // Setup exchange
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, methodOutcome);
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, "Observation");
        exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, "Patient/789");
        
        // Execute
        patientUtils.getPatientIdAndResourceIdFromOutCome(exchange);
        
        // Verify
        assertEquals("Observation/456", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertEquals("Patient/789", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void getPseudonymWithValidIdentifier() {
        Identifier identifier = new Identifier();
        identifier.setValue("8c4272e5-937c-4461-8df1-02b9df4cad23");
        Patient patient = new Patient();
        patient.addIdentifier(identifier);
        
        Identifier result = PatientUtils.getPseudonym(patient);
        assertNotNull(result);
        assertEquals("8c4272e5-937c-4461-8df1-02b9df4cad23", result.getValue());
    }

    @Test
    void getPseudonymWithNoIdentifier() {
        Patient patient = new Patient();
        assertThrows(InvalidRequestException .class, () -> {
            PatientUtils.getPseudonym(patient);
        }, "Patient must have an identifier");
    }

    @Test
    void getPatientIdFromPatientResource_WithValidIdentifier() {
        // Create a Patient resource with identifier
        Patient patient = new Patient();
        Identifier identifier = new Identifier()
            .setSystem("system1")
            .setValue("value1");
        patient.addIdentifier(identifier);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, patient);

        // Execute
        patientUtils.getPatientIdFromPatientResource(exchange);

        // Verify
        assertEquals("system1|value1", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
    }

    @Test
    void getPatientIdFromPatientResource_WithExistingPatient() {
        // Create a Patient resource with identifier
        Patient patient = new Patient();
        Identifier identifier = new Identifier()
            .setSystem("system1")
            .setValue("value1");
        patient.addIdentifier(identifier);

        // Mock repository to return existing patient
        PatientEhr mockPatientEhr = mock(PatientEhr.class);
        when(mockPatientEhr.getInternalPatientId()).thenReturn("existingId");
        when(patientEhrRepository.findByInputPatientId("system1|value1")).thenReturn(mockPatientEhr);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, patient);

        // Execute and verify exception
        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () -> 
            patientUtils.extractPatientIdOrIdentifier(exchange)
        );
        assertEquals("Patient: system1|value1 absolute reference already exists. Please provide relative reference: existingId", 
            exception.getMessage());
    }

    @Test
    void getPatientIdFromPatientResource_WithNoIdentifier() {
        // Create a Patient resource without identifier
        Patient patient = new Patient();

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, patient);

        // Execute
        patientUtils.getPatientIdFromPatientResource(exchange);

        // Verify
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
    }

    @Test
    void getPatientIdAndResourceIdFromResponse_WithValidBundle() {
        // Create a Bundle with a Patient resource response
        Bundle responseBundle = new Bundle();
        Bundle.BundleEntryComponent entry = responseBundle.addEntry();
        entry.getResponse().setLocation("Patient/123/_history/1");

        // Set up exchange
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, responseBundle);

        // Execute
        patientUtils.getPatientIdAndResourceIdFromResponse(exchange);

        // Verify
        assertEquals("Patient/123/_history/1", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void getPatientIdAndResourceIdFromResponse_WithExistingPatientId() {
        // Create a Bundle with a non-Patient resource response
        Bundle responseBundle = new Bundle();
        Bundle.BundleEntryComponent entry = responseBundle.addEntry();
        entry.getResponse().setLocation("Observation/456");

        // Set up exchange with existing patient ID
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, responseBundle);
        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, "Patient/123");

        // Execute
        patientUtils.getPatientIdAndResourceIdFromResponse(exchange);

        // Verify
        assertEquals("Observation/456", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void getPatientIdAndResourceIdFromResponse_WithMultipleEntries() {
        // Create a Bundle with multiple entries including a Patient
        Bundle responseBundle = new Bundle();
        
        // Add first entry (non-Patient)
        Bundle.BundleEntryComponent entry1 = responseBundle.addEntry();
        entry1.getResponse().setLocation("Observation/456");

        // Add second entry (Patient)
        Bundle.BundleEntryComponent entry2 = responseBundle.addEntry();
        entry2.getResponse().setLocation("Patient/123/_history/1");

        // Set up exchange
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, responseBundle);

        // Execute
        patientUtils.getPatientIdAndResourceIdFromResponse(exchange);

        // Verify
        assertEquals("Observation/456", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void getPatientIdAndResourceIdFromResponse_WithEmptyBundle() {
        // Create empty Bundle
        Bundle responseBundle = new Bundle();

        // Set up exchange
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, responseBundle);

        // Execute
        patientUtils.getPatientIdAndResourceIdFromResponse(exchange);

        // Verify
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void getPatientIdAndResourceIdFromResponse_WithNullBundle() {
        // Set up exchange with null bundle
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, null);

        // Execute
        patientUtils.getPatientIdAndResourceIdFromResponse(exchange);

        // Verify
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void extractPatientReference_WithBundle() {
        // Create a Bundle with multiple entries
        Bundle bundle = new Bundle();
        
        // Add an Observation without patient reference
        Observation obs1 = new Observation();
        Bundle.BundleEntryComponent entry1 = bundle.addEntry();
        entry1.setResource(obs1);
        
        // Add an Observation with patient reference
        Observation obs2 = new Observation();
        obs2.setSubject(new Reference("Patient/123"));
        Bundle.BundleEntryComponent entry2 = bundle.addEntry();
        entry2.setResource(obs2);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, bundle);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("RELATIVE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientReference_WithBundleNoPatientReference() {
        // Create a Bundle with entries but no patient references
        Bundle bundle = new Bundle();
        
        // Add Observations without patient references
        Observation obs1 = new Observation();
        Bundle.BundleEntryComponent entry1 = bundle.addEntry();
        entry1.setResource(obs1);
        
        Observation obs2 = new Observation();
        Bundle.BundleEntryComponent entry2 = bundle.addEntry();
        entry2.setResource(obs2);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, bundle);

        // Execute and verify exception
        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () -> 
            patientUtils.extractPatientIdOrIdentifier(exchange)
        );
        assertEquals("Bundle does not contain any resources with patient references", exception.getMessage());
    }

    @Test
    void extractPatientReference_WithEmptyBundle() {
        // Create empty Bundle
        Bundle bundle = new Bundle();

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, bundle);

        // Execute and verify exception
        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () -> 
            patientUtils.extractPatientIdOrIdentifier(exchange)
        );
        assertEquals("Bundle does not contain any resources with patient references", exception.getMessage());
    }

    @Test
    void extractPatientReference_WithConsent() {
        // Create a Consent resource with patient reference
        Consent consent = new Consent();
        consent.setPatient(new Reference("Patient/123"));

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, consent);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("RELATIVE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientReference_WithImmunization() {
        // Create an Immunization resource with patient reference
        Immunization immunization = new Immunization();
        immunization.setPatient(new Reference("Patient/123"));

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, immunization);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("RELATIVE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientReference_WithResearchSubject() {
        // Create a ResearchSubject resource with individual reference
        ResearchSubject researchSubject = new ResearchSubject();
        researchSubject.setIndividual(new Reference("Patient/123"));

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, researchSubject);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("RELATIVE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    
    @Test
    void extractPatientReference_WithBundleMixedContent() {
        // Create a Bundle with multiple entries of different types
        Bundle bundle = new Bundle();
        
        // Add a Basic resource without subject
        Consent consent = new Consent();
        bundle.addEntry().setResource(consent);
        
        
        // Add an Observation with patient reference
        Observation obs = new Observation();
        obs.setSubject(new Reference("Patient/123"));
        bundle.addEntry().setResource(obs);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, bundle);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("RELATIVE_REFERENCE", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    @Test
    void extractPatientIdOrIdentifier_WithPatientNoIdentifier() {
        // Create a Patient resource without identifier
        Patient patient = new Patient();

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, patient);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify - should set null patientId but still mark as ABSOLUTE_PATIENT
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
        assertEquals("ABSOLUTE_PATIENT", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
    }

    
    @Test
    void extractPatientIdOrIdentifier_WithSubjectIdentifierNoReference() {
        // Create an Observation with subject identifier but no reference
        Observation observation = new Observation();
        Reference subject = new Reference();
        Identifier identifier = new Identifier()
            .setSystem("system1")
            .setValue("value1");
        subject.setIdentifier(identifier);
        observation.setSubject(subject);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, observation);

        // Execute
        patientUtils.extractPatientIdOrIdentifier(exchange);

        // Verify
        assertEquals("system1|value1", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID));
        assertEquals("IDENTIFIER", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE));
        TokenParam tokenParam = exchange.getProperty(CamelConstants.IDENTIFIER_OBJECT, TokenParam.class);
        assertNotNull(tokenParam);
        assertEquals("value1", exchange.getIn().getHeader(CamelConstants.IDENTIFIER_STRING));
    }

    @Test
    void extractPatientIdOrIdentifier_WithExistingPatient() {
        // Create a Patient resource with identifier
        Patient patient = new Patient();
        Identifier identifier = new Identifier()
            .setSystem("system1")
            .setValue("value1");
        patient.addIdentifier(identifier);

        // Mock repository to return existing patient
        PatientEhr mockPatientEhr = mock(PatientEhr.class);
        when(mockPatientEhr.getInternalPatientId()).thenReturn("existingId");
        when(patientEhrRepository.findByInputPatientId("system1|value1")).thenReturn(mockPatientEhr);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, patient);

        // Execute and verify exception
        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () -> 
            patientUtils.extractPatientIdOrIdentifier(exchange)
        );
        assertEquals("Patient: system1|value1 absolute reference already exists. Please provide relative reference: existingId", 
            exception.getMessage());
    }

    @Test
    void extractPatientIdOrIdentifier_WithNoSubject() {
        // Create an Observation without any subject
        Observation observation = new Observation();

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, observation);

        // Execute and verify exception
        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () -> 
            patientUtils.extractPatientIdOrIdentifier(exchange)
        );
        assertEquals("Observation should be linked to a subject/patient", exception.getMessage());
    }
}

