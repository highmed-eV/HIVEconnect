package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositionLookupProcessorTest {

    @Mock
    private ResourceCompositionRepository resourceCompositionRepository;

    private CompositionLookupProcessor compositionLookupProcessor;
    private Exchange exchange;
    private UUID testEhrId;

    @BeforeEach
    void setUp() {
        compositionLookupProcessor = new CompositionLookupProcessor(resourceCompositionRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        testEhrId = UUID.randomUUID();
        exchange.getMessage().setHeader(CompositionConstants.EHR_ID, testEhrId);
    }

    @Test
    void processWithValidResourceIds() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7");
        exchange.setProperty(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, inputResourceIds);
        exchange.getMessage().setHeader(CamelConstants.REQUEST_HTTP_METHOD, "POST");

        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId))
                .thenReturn(Arrays.asList("07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1"));
        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("Organization/7", testEhrId))
                .thenReturn(Arrays.asList("28da67b8-426e-4eb7-bcc5-8be85d9284e7::local.ehrbase.org::1"));

        assertDoesNotThrow(() -> compositionLookupProcessor.process(exchange));
        
        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId);
        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("Organization/7", testEhrId);
    }

    @Test
    void processWithSameCompositionIdsShouldThrowException() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7");
        exchange.setProperty(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, inputResourceIds);
        exchange.getMessage().setHeader(CamelConstants.REQUEST_HTTP_METHOD, "POST");

        String compositionId = "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1";
        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId))
                .thenReturn(Arrays.asList(compositionId));
        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("Organization/7", testEhrId))
                .thenReturn(Arrays.asList(compositionId));

        when(resourceCompositionRepository.findInputResourcesByCompositionIdAndEhrId(compositionId, testEhrId))
                .thenReturn(Arrays.asList("Encounter/6", "Organization/7"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compositionLookupProcessor.process(exchange);
        });

        assertEquals("The input resource or input bundle contains resource(s) that already exist in the database. No new resources detected.", exception.getMessage());
        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId);
        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("Organization/7", testEhrId);
        verify(resourceCompositionRepository, times(1)).findInputResourcesByCompositionIdAndEhrId(compositionId, testEhrId);
    }

    @Test
    void processWithSingleCompositionIdShouldThrowException() throws Exception {
        List<String> inputResourceIds = List.of("Encounter/6");
        exchange.setProperty(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, inputResourceIds);
        exchange.getMessage().setHeader(CamelConstants.REQUEST_HTTP_METHOD, "POST");

        String compositionId = "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1";
        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId))
                .thenReturn(Arrays.asList(compositionId));

        when(resourceCompositionRepository.findInputResourcesByCompositionIdAndEhrId(compositionId, testEhrId))
                .thenReturn(Arrays.asList("Encounter/6"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compositionLookupProcessor.process(exchange);
        });

        assertEquals("The input resource or input bundle contains resource(s) that already exist in the database. No new resources detected.", exception.getMessage());
        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId);
        verify(resourceCompositionRepository, times(1)).findInputResourcesByCompositionIdAndEhrId(compositionId, testEhrId);
    }

    @Test
    void processWithNewResourceShouldNotThrowException() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7", "NewResource/8");
        exchange.setProperty(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, inputResourceIds);
        exchange.getMessage().setHeader(CamelConstants.REQUEST_HTTP_METHOD, "POST");

        String compositionId = "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1";
        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId))
                .thenReturn(Arrays.asList(compositionId));
        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("Organization/7", testEhrId))
                .thenReturn(Arrays.asList(compositionId));
        when(resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId("NewResource/8", testEhrId))
                .thenReturn(Arrays.asList());

        compositionLookupProcessor.process(exchange);

        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("Encounter/6", testEhrId);
        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("Organization/7", testEhrId);
        verify(resourceCompositionRepository, times(1)).findCompositionIdsByInputResourceIdAndEhrId("NewResource/8", testEhrId);
    }
}

