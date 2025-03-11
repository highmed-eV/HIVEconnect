package org.ehrbase.fhirbridge.ehr.camel;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.config.DebugProperties;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProvideResourceResponseProcessorTest {

    @Mock
    private ResourceCompositionRepository resourceCompositionRepository;

    @Mock
    private ProvideResourceResponseProcessor provideResourceResponseProcessor;

    @Mock
    private Exchange exchange;

    @Mock
    DebugProperties debugProperties;

    private FhirContext fhirContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fhirContext = FhirContext.forR4();
        provideResourceResponseProcessor = spy(new ProvideResourceResponseProcessor(resourceCompositionRepository, debugProperties));
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithPatientResource() throws Exception {
        Message message = new DefaultMessage(exchange.getContext());
        message.setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, "Patient");
        
        Patient patient = new Patient();
        patient.setId("test-patient-id");
        Identifier identifier = new Identifier();
        identifier.setSystem("http://test.system");
        identifier.setValue("12345");
        patient.addIdentifier(identifier);
        
        message.setHeader(CamelConstants.REQUEST_RESOURCE, patient);
        
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(patient);
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, outcome);
        exchange.setIn(message);

        provideResourceResponseProcessor.process(exchange);

        MethodOutcome responseOutcome = exchange.getIn().getBody(MethodOutcome.class);
        assertNotNull(responseOutcome);
        assertEquals(patient, responseOutcome.getResource());
    }

    @Test
    void processWithSingleResource() throws Exception {
        Message message = new DefaultMessage(exchange.getContext());
        message.setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, "Observation");
        
        Observation observation = new Observation();
        observation.setId("test-observation-id");
        message.setHeader(CamelConstants.REQUEST_RESOURCE, observation);
        
        MethodOutcome outcome = new MethodOutcome();
        IdType id = new IdType("Observation", "123", "1");
        outcome.setId(id);
        outcome.setResource(observation);
        
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, outcome);
        exchange.setIn(message);
        
        Composition composition = new Composition();
        composition.setUid(new ObjectVersionId("test-composition-id"));
        exchange.getIn().setHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME_COMPOSITION, composition);

        when(resourceCompositionRepository.findByInputResourceId(anyString())).thenReturn(Optional.empty());

        provideResourceResponseProcessor.process(exchange);

        ArgumentCaptor<ResourceComposition> captor = ArgumentCaptor.forClass(ResourceComposition.class);
        verify(resourceCompositionRepository).save(captor.capture());
        
        ResourceComposition saved = captor.getValue();
        assertEquals("test-observation-id", saved.getInputResourceId());
        assertEquals("Observation/123", saved.getInternalResourceId());
        assertEquals("test-composition-id", saved.getCompositionId());
    }

    @Test
    void processWithBundleResource() throws Exception {
        String bundleJson = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[" +
                "{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"pat1\",\"identifier\":[{\"system\":\"http://test.system\",\"value\":\"12345\"}]}}," +
                "{\"resource\":{\"resourceType\":\"Observation\",\"id\":\"obs1\",\"status\":\"final\"}}]}";
        
        Bundle inputBundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleJson);
        
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, "Bundle");
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, inputBundle);

        String responseJson = "{\"resourceType\":\"Bundle\",\"type\":\"transaction-response\",\"entry\":[" +
                "{\"response\":{\"status\":\"201\",\"location\":\"Patient/pat1-internal/_history/1\"}}," +
                "{\"response\":{\"status\":\"201\",\"location\":\"Observation/obs1-internal/_history/1\"}}]}";
        
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, responseJson);

        Composition composition = new Composition();
        composition.setUid(new ObjectVersionId("test-composition-id"));
        exchange.getIn().setHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME_COMPOSITION, composition);

        when(resourceCompositionRepository.findByInputResourceId(anyString())).thenReturn(Optional.empty());

        provideResourceResponseProcessor.process(exchange);

        ArgumentCaptor<ResourceComposition> captor = ArgumentCaptor.forClass(ResourceComposition.class);
        verify(resourceCompositionRepository, times(2)).save(captor.capture());

        var savedResources = captor.getAllValues();
        assertEquals(2, savedResources.size());
        
        // Verify first resource (Patient)
        assertEquals("http://test.system|12345", savedResources.get(0).getInputResourceId());
        assertEquals("Patient/pat1-internal", savedResources.get(0).getInternalResourceId());
        
        // Verify second resource (Observation)
        assertEquals("Observation/obs1", savedResources.get(1).getInputResourceId());
        assertEquals("Observation/obs1-internal", savedResources.get(1).getInternalResourceId());
    }

    @Test
    void processWithBundleResourceAndBundleResponse() throws Exception {
        Bundle inputBundle = createTestInputBundle();
        Bundle responseBundle = createTestResponseBundle();
        
        setupBundleExchange(inputBundle);
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, responseBundle);

        testBundleProcessing();

        // Verify that the exchange property was updated with JSON string
        Bundle updatedProperty = (Bundle) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
        assertEquals(responseBundle.getEntry().size(), updatedProperty.getEntry().size());
    }

    
    @Test
    void processWithBundleResourceAndStringResponse() throws Exception {
        Bundle inputBundle = createTestInputBundle();
        setupBundleExchange(inputBundle);
        
        String responseJson = "{\"resourceType\":\"Bundle\",\"type\":\"transaction-response\",\"entry\":[" +
                "{\"response\":{\"status\":\"201\",\"location\":\"Patient/pat1-internal/_history/1\"}}," +
                "{\"response\":{\"status\":\"201\",\"location\":\"Observation/obs1-internal/_history/1\"}}]}";
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, responseJson);

        testBundleProcessing();

        // Verify that the exchange property remains a string
        Object updatedProperty = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
        assertTrue(updatedProperty instanceof String);
        assertEquals(responseJson, updatedProperty);
    }

    @Test
    void processWithBundleResourceAndInvalidResponse() {
        Bundle inputBundle = createTestInputBundle();
        setupBundleExchange(inputBundle);
        
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, new Integer(42));

        assertThrows(RuntimeException.class, () -> provideResourceResponseProcessor.process(exchange));
    }

    @Test
    void processWithBundleResourceAndInvalidMethodOutcome() {
        Bundle inputBundle = createTestInputBundle();
        setupBundleExchange(inputBundle);
        
        MethodOutcome methodOutcome = new MethodOutcome();
        // Set null resource
        methodOutcome.setResource(null);
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, methodOutcome);

        Exception exception = assertThrows(RuntimeException.class, () -> 
            provideResourceResponseProcessor.process(exchange));
        assertTrue(exception.getMessage().contains("MethodOutcome does not contain a resource"));
    }

    @Test
    void processWithBundleResourceAndInvalidResourceType() {
        Bundle inputBundle = createTestInputBundle();
        setupBundleExchange(inputBundle);
        
        // Create a MethodOutcome with non-Bundle resource
        MethodOutcome methodOutcome = new MethodOutcome();
        Patient patient = new Patient();
        patient.setId("test-id");
        methodOutcome.setResource(patient);
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, methodOutcome);

        Exception exception = assertThrows(RuntimeException.class, () -> 
            provideResourceResponseProcessor.process(exchange));
        assertTrue(exception.getMessage().contains("Expected Bundle"));
    }

    @Test
    void processWithBundleResourceAndInvalidJsonString() {
        Bundle inputBundle = createTestInputBundle();
        setupBundleExchange(inputBundle);
        
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, "invalid json string");

        Exception exception = assertThrows(RuntimeException.class, () -> 
            provideResourceResponseProcessor.process(exchange));
        assertTrue(exception.getMessage().contains("Failed to parse"));
    }

    private Bundle createTestInputBundle() {
        String bundleJson = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[" +
                "{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"pat1\",\"identifier\":[{\"system\":\"http://test.system\",\"value\":\"12345\"}]}}," +
                "{\"resource\":{\"resourceType\":\"Observation\",\"id\":\"obs1\",\"status\":\"final\"}}]}";
        return fhirContext.newJsonParser().parseResource(Bundle.class, bundleJson);
    }

    private Bundle createTestResponseBundle() {
        String responseJson = "{\"resourceType\":\"Bundle\",\"type\":\"transaction-response\",\"entry\":[" +
                "{\"response\":{\"status\":\"201\",\"location\":\"Patient/pat1-internal/_history/1\"}}," +
                "{\"response\":{\"status\":\"201\",\"location\":\"Observation/obs1-internal/_history/1\"}}]}";
        return fhirContext.newJsonParser().parseResource(Bundle.class, responseJson);
    }

    private void setupBundleExchange(Bundle inputBundle) {
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, "Bundle");
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, inputBundle);
        
        Composition composition = new Composition();
        composition.setUid(new ObjectVersionId("test-composition-id"));
        exchange.getIn().setHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME_COMPOSITION, composition);

        when(resourceCompositionRepository.findByInputResourceId(anyString())).thenReturn(Optional.empty());
    }

    private void testBundleProcessing() throws Exception {
        provideResourceResponseProcessor.process(exchange);

        ArgumentCaptor<ResourceComposition> captor = ArgumentCaptor.forClass(ResourceComposition.class);
        verify(resourceCompositionRepository, times(2)).save(captor.capture());

        var savedResources = captor.getAllValues();
        assertEquals(2, savedResources.size());
        
        // Verify first resource (Patient)
        assertEquals("http://test.system|12345", savedResources.get(0).getInputResourceId());
        assertEquals("Patient/pat1-internal", savedResources.get(0).getInternalResourceId());
        
        // Verify second resource (Observation)
        assertEquals("Observation/obs1", savedResources.get(1).getInputResourceId());
        assertEquals("Observation/obs1-internal", savedResources.get(1).getInternalResourceId());

        // Verify response
        MethodOutcome responseOutcome = exchange.getMessage().getBody(MethodOutcome.class);
        assertNotNull(responseOutcome);
        assertTrue(responseOutcome.getCreated());
        assertNotNull(responseOutcome.getResource());
        assertTrue(responseOutcome.getResource() instanceof Bundle);
    }

    @Test
    void testExtractResourceId() throws Exception {
        Method method = ProvideResourceResponseProcessor.class.getDeclaredMethod("extractResourceId", Resource.class);
        method.setAccessible(true);

        // Test Patient resource with identifier
        Patient patient = new Patient();
        Identifier identifier = new Identifier();
        identifier.setSystem("http://test.system");
        identifier.setValue("12345");
        patient.addIdentifier(identifier);
        String patientId = (String) method.invoke(provideResourceResponseProcessor, patient);
        assertEquals("http://test.system|12345", patientId);

        // Test Patient resource without identifier
        Patient patientNoId = new Patient();
        String patientNoIdResult = (String) method.invoke(provideResourceResponseProcessor, patientNoId);
        assertNull(patientNoIdResult);

        // Test non-Patient resource
        Observation obs = new Observation();
        obs.setId("test-obs-id");
        String obsId = (String) method.invoke(provideResourceResponseProcessor, obs);
        assertEquals("Observation/test-obs-id", obsId);
    }

    @Test
    void testValidateAndUpdateMap() throws Exception {
        Method method = ProvideResourceResponseProcessor.class.getDeclaredMethod("validateAndUpdateMap", 
            Map.class, String.class, String.class);
        method.setAccessible(true);

        Map<String, String> resourceIdMap = new LinkedHashMap<>();

        // Test valid update
        method.invoke(provideResourceResponseProcessor, resourceIdMap, "test-id", "internal-id");
        assertEquals("internal-id", resourceIdMap.get("test-id"));

        // Test null inputResourceId
        assertThrows(Exception.class, () -> 
            method.invoke(provideResourceResponseProcessor, resourceIdMap, null, "internal-id"));

        // Test empty inputResourceId
        assertThrows(Exception.class, () -> 
            method.invoke(provideResourceResponseProcessor, resourceIdMap, "", "internal-id"));

        // Test conflict
        resourceIdMap.put("conflict-id", "existing-id");
        assertThrows(Exception.class, () -> 
            method.invoke(provideResourceResponseProcessor, resourceIdMap, "conflict-id", "different-id"));
    }
}
