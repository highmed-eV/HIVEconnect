package org.ehrbase.fhirbridge.fhir.camel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistingResourceReferenceProcessorTest {

    @Mock
    private ResourceCompositionRepository resourceCompositionRepository;

    @Mock
    private Exchange exchange;

    @Mock
    private ExistingResourceReferenceProcessor existingResourceReferenceProcessor;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        existingResourceReferenceProcessor = new ExistingResourceReferenceProcessor(resourceCompositionRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        objectMapper = new ObjectMapper();
    }

    @Test
    void processWithExistingResources() throws Exception {
        String existingResource1 = "{\"resourceType\":\"Patient\",\"id\":\"1\",\"name\":[{\"family\":\"Doe\"}]}";
        String existingResource2 = "{\"resourceType\":\"Observation\",\"id\":\"2\",\"status\":\"final\"}";

        List<String> existingResources = Arrays.asList(existingResource1, existingResource2);
        exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, existingResources);

        String inputResourceBundle = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"fullUrl\": \"Patient/101\", \"resource\": { \"resourceType\": \"Patient\", \"id\": \"101\",\"name\":[{\"family\":\"Doe\"}] } } ] }";
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, inputResourceBundle);

        when(resourceCompositionRepository.findInternalResourceIdByInputResourceId("Patient/1")).thenReturn("Patient/101");
        when(resourceCompositionRepository.findInternalResourceIdByInputResourceId("Observation/2")).thenReturn("Observation/102");

        existingResourceReferenceProcessor.process(exchange);

        // Verify the updated bundle
        String updatedBundleJson = exchange.getIn().getBody(String.class);
        JsonNode rootNode = objectMapper.readTree(updatedBundleJson);
        ArrayNode entryArray = (ArrayNode) rootNode.get("entry");
        assertEquals(2, entryArray.size()); // 1 entry already present + 1 new entry for the existing resource
        assertTrue(updatedBundleJson.contains("Patient/101")); // Check if the existing resource ID was replaced
    }

    @Test
    void processWithNoExistingResources() throws Exception {
        exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, null);

        String inputResourceBundle = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"resource\": { \"resourceType\": \"Patient\", \"id\": \"101\" } } ] }";
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, inputResourceBundle);

        existingResourceReferenceProcessor.process(exchange);

        // Verify no change in the bundle
        String updatedBundleJson = exchange.getIn().getBody(String.class);
        assertNotNull(updatedBundleJson);

        // Normalize both JSON strings for comparison (ignoring formatting differences)
        JsonNode inputNode = objectMapper.readTree(inputResourceBundle);
        JsonNode updatedNode = objectMapper.readTree(updatedBundleJson);
        String inputJsonNormalized = objectMapper.writeValueAsString(inputNode);
        String updatedJsonNormalized = objectMapper.writeValueAsString(updatedNode);

        // Assert that the normalized JSON strings are equal (no new resources added)
        assertEquals(inputJsonNormalized, updatedJsonNormalized);
    }

    @Test
    void processWithInvalidBundle() throws Exception {
        // Prepare mock data with invalid input (not a Bundle)
        String invalidInputResourceBundle = "{ \"resourceType\": \"Patient\", \"id\": \"1\" }";
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, invalidInputResourceBundle);

        existingResourceReferenceProcessor.process(exchange);

        // No change expected
        assertNull(exchange.getIn().getBody());
    }
}

