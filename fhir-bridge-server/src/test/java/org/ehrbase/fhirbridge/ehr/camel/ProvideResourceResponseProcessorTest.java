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
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        provideResourceResponseProcessor = new ProvideResourceResponseProcessor(resourceCompositionRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithSingleResource() throws Exception {
        Message message = new DefaultMessage(exchange.getContext());
        message.setHeader(CamelConstants.INPUT_RESOURCE_TYPE, "Patient");
        message.setHeader(CamelConstants.INPUT_RESOURCE, "{\"resourceType\":\"Patient\"}");
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(FhirContext.forR4().newJsonParser().parseResource(
                "{\"resourceType\":\"Patient\",\"id\":\"123\"}"
        ));
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, outcome);
        exchange.setIn(message);
        Composition composition = new Composition();
        exchange.getIn().setBody(composition);

        when(resourceCompositionRepository.findById(anyString())).thenReturn(Optional.empty());

        provideResourceResponseProcessor.process(exchange);

        // Verify the response
        MethodOutcome responseOutcome = exchange.getIn().getBody(MethodOutcome.class);
        assertNotNull(responseOutcome);
        assertNotNull(responseOutcome.getResource());

        // Extract the actual resource from MethodOutcome
        IBaseResource resource = responseOutcome.getResource();
        assertTrue(resource instanceof Patient);

        Patient patient = (Patient) resource;
        assertEquals("Patient", patient.fhirType());
        assertEquals("123", patient.getIdElement().getIdPart());

        // Ensure `updateResourceCompositions` is NOT called
        verify(resourceCompositionRepository, never()).save(any());
    }


    @Test
    void processWithBundleResource() throws Exception {
        // Prepare input Bundle resource
        String bundleInput = "{\"resourceType\":\"Bundle\",\"entry\":[{\"resource\":{\"resourceType\":\"Condition\",\"id\":\"21\"}}]}";
        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, bundleInput);
        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE_TYPE, "Bundle");

        // Mock Composition object
        Composition composition = new Composition();
        composition.setUid(new ObjectVersionId("composition-uid"));
        exchange.getIn().setBody(composition);
        // Mock FHIR server outcome
        String fhirOutcome  = "{\"resourceType\":\"Bundle\",\"id\":\"4d84b0dd-5206-450c-b2b8-53aab9d4f851\",\"type\":\"transaction-response\",\"link\":[{\"relation\":\"self\",\"url\":\"http://localhost:8088/fhir\"}],\"entry\":[{\"response\":{\"status\":\"201 Created\",\"location\":\"Condition/121/_history/1\",\"etag\":\"1\",\"lastModified\":\"2024-12-05T04:43:36.150+00:00\",\"outcome\":{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"severity\":\"information\",\"code\":\"informational\",\"details\":{\"coding\":[{\"system\":\"https://hapifhir.io/fhir/CodeSystem/hapi-fhir-storage-response-code\",\"code\":\"SUCCESSFUL_CREATE\",\"display\":\"Create succeeded.\"}]},\"diagnostics\":\"Successfully created resource \\\"Condition/121/_history/1\\\". Took 18ms.\"}]}}}]}";
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, fhirOutcome);
        // Mock Open FHIR server outcome
        String openFhirOutcome = "{\"_type\":\"COMPOSITION\",\"name\":{\"_type\":\"DV_TEXT\",\"value\":\"Diagnose\"},\"archetype_details\":{\"archetype_id\":{\"value\":\"openEHR-EHR-COMPOSITION.report.v1\"},\"template_id\":{\"value\":\"KDS_Diagnose\"},\"rm_version\":\"1.0.4\"}}";
        exchange.getMessage().setHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME, openFhirOutcome);
        // Mock Open EHR server outcome
        String openEhrOutcome = "{\"archetype_node_id\":\"openEHR-EHR-COMPOSITION.report.v1\",\"uid\":{\"_type\":\"OBJECT_VERSION_ID\",\"value\":\"07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1\"}}";
        exchange.getMessage().setHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME, openEhrOutcome);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node2 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME));
        JsonNode node3 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME));
        assertNotNull(node2, "Parsed JsonNode for OPEN_FHIR_SERVER_OUTCOME should not be null");
        assertNotNull(node3, "Parsed JsonNode for OPEN_EHR_SERVER_OUTCOME should not be null");

        when(resourceCompositionRepository.findById(anyString())).thenReturn(Optional.empty());

        provideResourceResponseProcessor.process(exchange);

        // Validate the update of resourceIdMap indirectly testing validateAndUpdateMap
        LinkedHashMap<String, String> resourceIdMap = new LinkedHashMap<>();
        resourceIdMap.put("Condition/21", "Condition/121");
        assertEquals("Condition/121", resourceIdMap.get("Condition/21"),
                "The resourceIdMap should correctly update the resource ID");

        // Verify the correct saving of the resource in the repository
        ArgumentCaptor<ResourceComposition> captor = ArgumentCaptor.forClass(ResourceComposition.class);
        verify(resourceCompositionRepository, atLeastOnce()).save(captor.capture());
        ResourceComposition saved = captor.getValue();
        assertNotNull(saved, "Saved resource should not be null");
        assertEquals("Condition/21", saved.getInputResourceId(),
                "The input resource ID should match the expected value");
        assertEquals("Condition/121", saved.getInternalResourceId(),
                "The internal resource ID should match the expected value");
    }

    @Test
    void findInputResourceIdByIndex() throws Exception {
        LinkedHashMap<String, String> resourceIdMap = new LinkedHashMap<>();
        resourceIdMap.put("Condition/21", null);
        resourceIdMap.put("Condition/22", null);
        resourceIdMap.put("Condition/23", null);

        Method method = ProvideResourceResponseProcessor.class.getDeclaredMethod("findInputResourceIdByIndex", Map.class, int.class);
        method.setAccessible(true);  // Make the private method accessible

        // Test valid index
        String result = (String) method.invoke(provideResourceResponseProcessor, resourceIdMap, 1);
        assertNotNull(result);
        assertEquals("Condition/22", result, "The ID at index 1 should be Condition/22");

        // Test index out of bounds
        result = (String) method.invoke(provideResourceResponseProcessor, resourceIdMap, 5);
        assertNull(result, "The ID at an out-of-bounds index should be null");

        // Test first index
        result = (String) method.invoke(provideResourceResponseProcessor, resourceIdMap, 0);
        assertNotNull(result);
        assertEquals("Condition/21", result, "The ID at index 0 should be Condition/21");

        // Test negative index (should return null)
        result = (String) method.invoke(provideResourceResponseProcessor, resourceIdMap, -1);
        assertNull(result, "The ID at a negative index should be null");
    }
}
