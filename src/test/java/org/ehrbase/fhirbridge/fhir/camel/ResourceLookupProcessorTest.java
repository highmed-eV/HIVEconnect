package org.ehrbase.fhirbridge.fhir.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private ReferencedResourceLookupProcessor resourceLookupProcessor;

    @Mock
    private Exchange exchange;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resourceLookupProcessor = new ReferencedResourceLookupProcessor(resourceCompositionRepository, objectMapper);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithValidReferenceResourceIds() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7");
        exchange.setProperty(CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS, inputResourceIds);
        ResourceComposition resource1 = new ResourceComposition("Encounter/6", "Encounter/106", null, null, null);
        ResourceComposition resource2 = new ResourceComposition("Organization/7", "Organization/107", null, null, null);
        when(resourceCompositionRepository.findInternalResourceIdsByInputResourceIdsAndSystemId(inputResourceIds, "systemId"))
                .thenReturn(Arrays.asList("Encounter/106", "Organization/107"));
        when(resourceCompositionRepository.findByInputResourceIdAndSystemId("Encounter/6", "systemId")).thenReturn(Optional.of(resource1));
        when(resourceCompositionRepository.findByInputResourceIdAndSystemId("Organization/7", "systemId")).thenReturn(Optional.of(resource2));

        String inputResource = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"fullUrl\": \"Condition/6\", \"resource\": { \"resourceType\": \"Condition\", \"id\": \"6\", \"encounter\": { \"reference\": \"Encounter/6\" }, \"organization\": { \"reference\": \"Organization/7\" } } } ] }";
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, inputResource);
        exchange.getIn().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, "systemId");

        resourceLookupProcessor.process(exchange);

        verify(resourceCompositionRepository, times(1)).findInternalResourceIdsByInputResourceIdsAndSystemId(inputResourceIds, "systemId");
        verify(resourceCompositionRepository, times(1)).findByInputResourceIdAndSystemId("Encounter/6", "systemId");
        verify(resourceCompositionRepository, times(1)).findByInputResourceIdAndSystemId("Organization/7", "systemId");

        assertEquals(Arrays.asList("Encounter/106", "Organization/107"), exchange.getProperty(CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS));

        // Verify that the updated resource is set correctly
        String updatedResource =  (String) exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING);
        assertNotNull(updatedResource);
        assertTrue(updatedResource.contains("Encounter/106"));
        assertTrue(updatedResource.contains("Organization/107"));
    }

    @Test
    void processWithInvalidReferenceResourceIds() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "invalidResource");
        exchange.setProperty(CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS, inputResourceIds);
        ResourceComposition resource1 = new ResourceComposition("Encounter/6", "Encounter/106", null, null, null);

        when(resourceCompositionRepository.findInternalResourceIdsByInputResourceIdsAndSystemId(inputResourceIds, "systemId"))
                .thenReturn(Arrays.asList("Encounter/106"));
        when(resourceCompositionRepository.findByInputResourceIdAndSystemId("Encounter/6", "systemId")).thenReturn(Optional.of(resource1));

        String inputResource = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"fullUrl\": \"Condition/6\", \"resource\": { \"resourceType\": \"Condition\", \"id\": \"6\", \"encounter\": { \"reference\": \"Encounter/6\" }, \"organization\": { \"reference\": \"invalidResource\" } } } ] }";
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, inputResource);
        exchange.getIn().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, "systemId");

        resourceLookupProcessor.process(exchange);

        // Verify that the internalResourceIds list contains only valid ones
        verify(resourceCompositionRepository, times(1)).findInternalResourceIdsByInputResourceIdsAndSystemId(inputResourceIds, "systemId");
        verify(resourceCompositionRepository, times(1)).findByInputResourceIdAndSystemId("Encounter/6", "systemId");

        assertEquals(Arrays.asList("Encounter/106"), exchange.getProperty(CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS));

        // Verify that the updated resource is set correctly
        String updatedResource =  (String) exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING);
        assertNotNull(updatedResource);
        assertTrue(updatedResource.contains("Encounter/106"));
        assertTrue(updatedResource.contains("invalidResource"));
    }
}

