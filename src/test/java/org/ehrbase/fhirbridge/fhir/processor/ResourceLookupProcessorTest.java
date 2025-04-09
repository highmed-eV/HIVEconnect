package org.ehrbase.fhirbridge.fhir.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.ehrbase.fhirbridge.fhir.camel.CompositionLookupProcessor;
import org.ehrbase.fhirbridge.fhir.camel.ReferencedResourceLookupProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceLookupProcessorTest {

    private Exchange exchange;

    @Mock
    private ResourceCompositionRepository resourceCompositionRepository;

    private ReferencedResourceLookupProcessor referencedResourceLookupProcessor;
    private CompositionLookupProcessor compositionLookupProcessor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        exchange = new DefaultExchange(new DefaultCamelContext());
        objectMapper = new ObjectMapper();
        referencedResourceLookupProcessor = new ReferencedResourceLookupProcessor(resourceCompositionRepository, objectMapper);
        compositionLookupProcessor = new CompositionLookupProcessor(resourceCompositionRepository);
    }

    @Test
    void testReferencedResourceLookupProcessor_WithValidResourceIds() throws Exception {
        // Arrange
        String systemId = "test-system";
        List<String> inputResourceIds = Arrays.asList("resource1", "resource2");
        List<String> internalResourceIds = Arrays.asList("internal1", "internal2");
        String inputResource = "{\"entry\":[{\"resource\":{\"id\":\"resource1\"}},{\"resource\":{\"id\":\"resource2\"}}]}";

        exchange.getIn().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, systemId);
        exchange.setProperty(CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS, inputResourceIds);
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, inputResource);
        when(resourceCompositionRepository.findInternalResourceIdsByInputResourceIdsAndSystemId(inputResourceIds, systemId))
            .thenReturn(internalResourceIds);

        // Act
        referencedResourceLookupProcessor.process(exchange);

        // Assert
        List<String> updatedResourceIds = exchange.getProperty(CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS, List.class);
        assertEquals(internalResourceIds, updatedResourceIds);
        assertNotNull(exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING));
    }

    @Test
    void testReferencedResourceLookupProcessor_WithEmptyResourceIds() throws Exception {
        // Arrange
        exchange.setProperty(CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS, Collections.emptyList());

        // Act
        referencedResourceLookupProcessor.process(exchange);

        // Assert
        assertNull(exchange.getProperty(CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS));
        assertNull(exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING));
    }

    @Test
    void testReferencedResourceLookupProcessor_WithNullResourceIds() throws Exception {
        // Arrange
        exchange.setProperty(CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS, null);

        // Act
        referencedResourceLookupProcessor.process(exchange);

        // Assert
        assertNull(exchange.getProperty(CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS));
        assertNull(exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING));
    }

    

    @Test
    void testCompositionLookupProcessor_WithMultipleResources() throws Exception {
        // Arrange
        String systemId = "test-system";
        List<String> resourceIds = Arrays.asList("resource1", "resource2", "resource3");
        UUID compositionId = UUID.randomUUID();
        UUID ehrId = UUID.randomUUID();
        List<String> compositionIds = Arrays.asList(compositionId.toString());
        
        ResourceComposition resourceComposition = new ResourceComposition();
        resourceComposition.setCompositionId(compositionId.toString());
        resourceComposition.setEhrId(ehrId);
        resourceComposition.setInputResourceId(resourceIds.get(0));
        resourceComposition.setSystemId(systemId);

        exchange.getIn().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, systemId);
        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID, resourceIds);
        exchange.getIn().setHeader(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, resourceIds);
        exchange.getIn().setHeader(CompositionConstants.EHR_ID, ehrId);
        
       
        // Act
        compositionLookupProcessor.process(exchange);

        // Assert
        assertEquals(resourceIds, exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertEquals(systemId, exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID));
        assertEquals(resourceIds, exchange.getIn().getHeader(CamelConstants.FHIR_REQUEST_RESOURCE_IDS));
    }

    @Test
    void testCompositionLookupProcessor_WithNoCompositionFound() throws Exception {
        // Arrange
        String systemId = "test-system";
        String resourceId = "test-resource";
        UUID ehrId = UUID.randomUUID();
        List<String> requestResourceIds = Arrays.asList(resourceId);

        exchange.getIn().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, systemId);
        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID, resourceId);
        exchange.getIn().setHeader(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, requestResourceIds);
        exchange.getIn().setHeader(CompositionConstants.EHR_ID, ehrId);
        

        // Act
        compositionLookupProcessor.process(exchange);

        // Assert
        assertNull(exchange.getIn().getHeader(CamelConstants.OPENEHR_COMPOSITION_ID));
        assertEquals(resourceId, exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID));
        assertEquals(systemId, exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID));
        assertEquals(requestResourceIds, exchange.getIn().getHeader(CamelConstants.FHIR_REQUEST_RESOURCE_IDS));
    }

    @Test
    void testCompositionLookupProcessor_WithNullResourceId() throws Exception {
        // Arrange
        exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID, null);

        // Act
        compositionLookupProcessor.process(exchange);

        // Assert
        assertNull(exchange.getIn().getHeader(CamelConstants.OPENEHR_COMPOSITION_ID));
        verify(resourceCompositionRepository, never()).findCompositionIdsByInternalResourceIdAndEhrId(anyString(), any());
    }
} 