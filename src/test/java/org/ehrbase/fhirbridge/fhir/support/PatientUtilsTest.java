package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientUtilsTest {

    @Mock
    private PatientEhrRepository patientEhrRepository;

    @Mock
    private Exchange exchange;

    @Mock
    private Message inMessage;

    @Mock
    private MethodOutcome methodOutcome;

    @Mock
    private PatientUtils patientUtils;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        patientUtils = new PatientUtils(patientEhrRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        objectMapper = new ObjectMapper();
    }

    @Test
    void extractPatientIdOrIdentifierWithPatientResource() {
        String resourceJson = "{\"resourceType\":\"Patient\", \"identifier\":[{\"system\":\"system1\",\"value\":\"value1\"}]}";
        exchange.getIn().setBody(resourceJson);
        exchange.getIn().setHeader(CamelConstants.PATIENT_ID, "system1|value1");
        patientUtils.extractPatientIdOrIdentifier(exchange);
        String  patientId = exchange.getIn().getHeader(CamelConstants.PATIENT_ID, String.class);
        assertEquals("system1|value1", patientId);
    }

    @Test
    void getPatientInfoResourceWithValidBundleInPatient() throws Exception {
        // Given a valid Bundle with a Patient resource
        String resourceJson = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"resource\": { \"resourceType\": \"Patient\", \"id\": \"123\" } } ] }";
        String expectedPatientJson = "{ \"resourceType\" : \"Patient\", \"id\" : \"123\" }";
        JsonNode expectedPatientNode = objectMapper.readTree(expectedPatientJson);
        JsonNode result = patientUtils.getPatientInfoResource(resourceJson);
        assertNotNull(result);
        assertEquals(expectedPatientNode, result);
    }

    @Test
    void getPatientInfoResourceWithValidBundle() throws Exception {
        String resourceJson = "{\"resourceType\": \"Bundle\", \"id\": \"example-bundle\", \"type\": \"transaction\", \"entry\": [{\"fullUrl\": \"urn:uuid:456\", \"resource\": {\"resourceType\": \"Condition\", \"id\": \"example-condition\", \"subject\": {\"reference\": \"Patient/123\"}}}]}";
        String expectedPatientJson = "{\"reference\" : \"Patient/123\"}";
        JsonNode expectedPatientNode = objectMapper.readTree(expectedPatientJson);
        JsonNode result = patientUtils.getPatientInfoResource(resourceJson);
        assertNotNull(result);
        assertEquals(expectedPatientNode, result);
    }

    @Test
    void getPatientInfoResourceWithSingleResource() throws Exception {
        // Given a valid single Patient resource
        String resourceJson = "{ \"resourceType\": \"Patient\", \"id\": \"123\" }";
        String expectedPatientJson = "{ \"resourceType\" : \"Patient\", \"id\" : \"123\" }";
        JsonNode expectedPatientNode = objectMapper.readTree(expectedPatientJson);
        JsonNode result = patientUtils.getPatientInfoResource(resourceJson);
        assertNotNull(result);
        assertEquals(expectedPatientNode, result);
    }

    @Test
    void extractPatientIdFromReference() throws Exception {
        String reference = "Patient/12345";
        String resourceJson = "{\"resourceType\": \"Bundle\", \"id\": \"example-bundle\", \"type\": \"transaction\", \"entry\": [{\"fullUrl\": \"urn:uuid:456\", \"resource\": {\"resourceType\": \"Condition\", \"id\": \"example-condition\", \"subject\": {\"reference\": \"Patient/123\"}}}]}";
        JsonNode resourceNode = objectMapper.readTree(resourceJson);
        String patientId = patientUtils.extractPatientIdFromReference(exchange, resourceNode, reference);
        assertEquals("Patient/12345", patientId);
        String patientIdType = exchange.getIn().getHeader(CamelConstants.PATIENT_ID_TYPE, String.class);
        assertEquals("RELATIVE_REFERENCE",patientIdType);
    }

    @Test
    void extractPatientIdFromReferenceContainedReference() throws Exception {
        String reference = "#patient1";
        String resourceJson = "{\"resourceType\": \"MedicationRequest\", \"subject\": {\"reference\": \"#patient1\"}, \"contained\": [{\"resourceType\": \"Patient\", \"id\": \"patient1\", \"name\": [{\"family\": \"Doe\", \"given\": [\"John\"]}]}]}";
        JsonNode resourceNode = objectMapper.readTree(resourceJson);
        String patientId = patientUtils.extractPatientIdFromReference(exchange, resourceNode, reference);
        assertEquals("patient1", patientId);
        String patientIdType = exchange.getIn().getHeader(CamelConstants.PATIENT_ID_TYPE, String.class);
        assertEquals("ABSOLUTE_REFERENCE",patientIdType);
    }

    @Test
    void extractPatientIdFromReferenceInternalReference() throws Exception {
        String reference = "urn:uuid:1";
        String resourceJson = "{\"resourceType\": \"Bundle\", \"type\": \"transaction\", \"entry\": [{\"fullUrl\": \"urn:uuid:1\", \"resource\": {\"resourceType\": \"Patient\", \"id\": \"patient1\", \"name\": [{\"family\": \"Doe\", \"given\": [\"John\"]}]}}, {\"fullUrl\": \"urn:uuid:2\", \"resource\": {\"resourceType\": \"MedicationRequest\", \"id\": \"medication1\", \"subject\": {\"reference\": \"urn:uuid:1\"}, \"medicationCodeableConcept\": {\"text\": \"Aspirin\"}}}]}";
        JsonNode resourceNode = objectMapper.readTree(resourceJson);
        String patientId = patientUtils.extractPatientIdFromReference(exchange, resourceNode, reference);
        assertEquals("patient1", patientId);
        String patientIdType = exchange.getIn().getHeader(CamelConstants.PATIENT_ID_TYPE, String.class);
        assertEquals("ABSOLUTE_REFERENCE",patientIdType);
    }

    @Test
    void extractPatientIdFromReferenceExternalReference() throws Exception {
        String reference = "http://external-fhir-server.com/Patient/456";
        String resourceJson = "{\"resourceType\": \"Bundle\", \"id\": \"example-bundle\", \"type\": \"transaction\", \"entry\": [{\"fullUrl\": \"urn:uuid:456\", \"resource\": {\"resourceType\": \"Condition\", \"id\": \"example-condition\", \"subject\": {\"reference\": \"http://external-fhir-server.com/Patient/456\"}}}]}";
        JsonNode resourceNode = objectMapper.readTree(resourceJson);
        String patientId = patientUtils.extractPatientIdFromReference(exchange, resourceNode, reference);
        assertEquals("http://external-fhir-server.com/Patient/456", patientId);
        String patientIdType = exchange.getIn().getHeader(CamelConstants.PATIENT_ID_TYPE, String.class);
        assertEquals("EXTERNAL_REFERENCE",patientIdType);
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
    void getPatientIdFromOutCome() {
        IIdType mockIdType = mock(IIdType.class);
        when(methodOutcome.getId()).thenReturn(mockIdType);
        when(mockIdType.getResourceType()).thenReturn("Patient");
        when(mockIdType.getIdPart()).thenReturn("123");
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, methodOutcome);
        patientUtils.getPatientIdFromOutCome(exchange);
        assertEquals("Patient/123", exchange.getIn().getHeader(CamelConstants.SERVER_PATIENT_ID));
        assertEquals(methodOutcome.getResource(), exchange.getIn().getHeader(CamelConstants.SERVER_PATIENT_RESOURCE));
    }

    @Test
    void getPatientIdFromPatientResource() {
        String responseString = "{\"identifier\":[{\"system\":\"system1\",\"value\":\"value1\"}]}";
        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, responseString);
        when(patientEhrRepository.findByInputPatientId("system1|value1")).thenReturn(null);
        patientUtils.getPatientIdFromPatientResource(exchange);
        assertEquals("system1|value1", exchange.getIn().getHeader(CamelConstants.PATIENT_ID));
    }

    @Test
    void getPseudonymWithValidIdentifier(){
        Identifier identifier = new Identifier();
        identifier.setValue("8c4272e5-937c-4461-8df1-02b9df4cad23");
        Patient patient = new Patient();
        patient.addIdentifier(identifier);
        Identifier result = PatientUtils.getPseudonym(patient);
        assertNotNull(result);
        assertEquals("8c4272e5-937c-4461-8df1-02b9df4cad23", result.getValue());
    }
}

