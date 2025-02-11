package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceLookupProcessorTest {

    @Mock
    private ResourceCompositionRepository resourceCompositionRepository;

    @Mock
    private ResourceLookupProcessor resourceLookupProcessor;

    @Mock
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        resourceLookupProcessor = new ResourceLookupProcessor(resourceCompositionRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithValidReferenceResourceIds() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7");
        exchange.setProperty(CamelConstants.REFERENCE_INPUT_RESOURCE_IDS, inputResourceIds);
        ResourceComposition resource1 = new ResourceComposition("Encounter/6", "Encounter/106");
        ResourceComposition resource2 = new ResourceComposition("Organization/7", "Organization/107");
        when(resourceCompositionRepository.findById("Encounter/6")).thenReturn(Optional.of(resource1));
        when(resourceCompositionRepository.findById("Organization/7")).thenReturn(Optional.of(resource2));
        String inputResource = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"fullUrl\": \"Condition/6\", \"resource\": { \"resourceType\": \"Condition\", \"id\": \"6\", \"encounter\": { \"reference\": \"Encounter/6\" }, \"organization\": { \"reference\": \"Organization/7\" } } } ] }";
        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, inputResource);

        resourceLookupProcessor.process(exchange);

        verify(resourceCompositionRepository, times(2)).findById("Encounter/6");
        verify(resourceCompositionRepository, times(2)).findById("Organization/7");
        assertEquals(Arrays.asList("Encounter/106", "Organization/107"), exchange.getProperty(CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS));

        // Verify that the updated resource is set correctly
        String updatedResource = exchange.getIn().getBody(String.class);
        assertNotNull(updatedResource);
        assertTrue(updatedResource.contains("Encounter/106"));
        assertTrue(updatedResource.contains("Organization/107"));
    }

    @Test
    void processWithInvalidReferenceResourceIds() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "invalidResource");
        exchange.setProperty(CamelConstants.REFERENCE_INPUT_RESOURCE_IDS, inputResourceIds);
        ResourceComposition resource1 = new ResourceComposition("Encounter/6", "Encounter/106");
        when(resourceCompositionRepository.findById("Encounter/6")).thenReturn(Optional.of(resource1));
        when(resourceCompositionRepository.findById("invalidResource")).thenReturn(Optional.empty());
        String inputResource = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"fullUrl\": \"Condition/6\", \"resource\": { \"resourceType\": \"Condition\", \"id\": \"6\", \"encounter\": { \"reference\": \"Encounter/6\" }, \"organization\": { \"reference\": \"invalidResource\" } } } ] }";
        exchange.getIn().setHeader(CamelConstants.INPUT_RESOURCE, inputResource);

        resourceLookupProcessor.process(exchange);

        // Verify that the internalResourceIds list contains only valid ones
        verify(resourceCompositionRepository, times(2)).findById("Encounter/6");
        verify(resourceCompositionRepository, times(1)).findById("invalidResource");
        assertEquals(Arrays.asList("Encounter/106"), exchange.getProperty(CamelConstants.REFERENCE_INTERNAL_RESOURCE_IDS));

        // Verify that the updated resource is set correctly
        String updatedResource = exchange.getIn().getBody(String.class);
        assertNotNull(updatedResource);
        assertTrue(updatedResource.contains("Encounter/106"));
        assertTrue(updatedResource.contains("invalidResource"));
    }
}

