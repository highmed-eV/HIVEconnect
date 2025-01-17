package org.ehrbase.fhirbridge.ehr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProvideResourceResponseProcessorTest {

    private ProvideResourceResponseProcessor provideResourceResponseProcessor;

    @Mock
    private ResourceCompositionRepository resourceCompositionRepository;

    @Mock
    private Logger logger;

    private Exchange exchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        provideResourceResponseProcessor = new ProvideResourceResponseProcessor(resourceCompositionRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithNonBundleResource() throws Exception {
        Message message = new DefaultMessage(exchange.getContext());
        message.setHeader(CamelConstants.INPUT_RESOURCE_TYPE, "Patient");
        message.setHeader(CamelConstants.INPUT_RESOURCE, "{\"resourceType\":\"Patient\"}");

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(FhirContext.forR4().newJsonParser().parseResource("{\"resourceType\":\"Patient\",\"id\":\"123\"}"));
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, outcome);
        exchange.setIn(message);

        when(resourceCompositionRepository.findById(anyString())).thenReturn(Optional.empty());

        provideResourceResponseProcessor.process(exchange);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode response = objectMapper.readTree(exchange.getIn().getBody(String.class));
        assertNotNull(response);
        assertEquals("Patient", response.get("resourceType").asText());
        assertEquals("123", response.get("id").asText());
    }

//    @Test
    void processWithBundleResource() throws Exception {
        String bundleInput = "{\"resourceType\":\"Bundle\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"123\"}}]}";
        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, bundleInput);
        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE_TYPE, "Bundle");

        assertNotNull(bundleInput, "Input resource must not be null");

        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, bundleInput);

        when(resourceCompositionRepository.findById(anyString())).thenReturn(Optional.empty());
        provideResourceResponseProcessor.process(exchange);

        ArgumentCaptor<ResourceComposition> captor = ArgumentCaptor.forClass(ResourceComposition.class);
        verify(resourceCompositionRepository, atLeastOnce()).save(captor.capture());
        ResourceComposition saved = captor.getValue();
        // Verify that the saved resource has the correct input resource ID
        assertNotNull(saved, "Saved resource should not be null");
        assertEquals("Patient/123", saved.getInputResourceId(), "The input resource ID should match the expected value");
    }

    @Test
    void validateAndUpdateMap() {
        var resourceIdMap = new java.util.LinkedHashMap<String, String>();
        resourceIdMap.put("Patient/1", null);
        provideResourceResponseProcessor.validateAndUpdateMap(resourceIdMap, "Patient/1", "Patient/123");
        assertEquals("Patient/123", resourceIdMap.get("Patient/1"));
    }
}
