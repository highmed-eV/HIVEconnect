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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositionLookupProcessorTest {

    @Mock
    private ResourceCompositionRepository resourceCompositionRepository;

    private CompositionLookupProcessor compositionLookupProcessor;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        compositionLookupProcessor = new CompositionLookupProcessor(resourceCompositionRepository);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithValidResourceIds() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7");
        exchange.setProperty(CamelConstants.INPUT_RESOURCE_IDS, inputResourceIds);

        ResourceComposition resource1 = new ResourceComposition("Encounter/6", null);
        ResourceComposition resource2 = new ResourceComposition("Organization/7", null);
        setCompositionId(resource1, "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1");
        setCompositionId(resource2, "28da67b8-426e-4eb7-bcc5-8be85d9284e7::local.ehrbase.org::1");

        when(resourceCompositionRepository.findById("Encounter/6")).thenReturn(Optional.of(resource1));
        when(resourceCompositionRepository.findById("Organization/7")).thenReturn(Optional.of(resource2));

        compositionLookupProcessor.process(exchange);
        verify(resourceCompositionRepository, times(1)).findById("Encounter/6");
        verify(resourceCompositionRepository, times(1)).findById("Organization/7");
    }

    private void setCompositionId(ResourceComposition resource, String compositionId) throws Exception {
        Field field = ResourceComposition.class.getDeclaredField("compositionId");
        field.setAccessible(true);
        field.set(resource, compositionId);
    }

    @Test
    void processWithSameCompositionIdsShouldThrowException() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7");
        exchange.setProperty(CamelConstants.INPUT_RESOURCE_IDS, inputResourceIds);
        exchange.setProperty(CamelConstants.INPUT_HTTP_METHOD, "POST");

        ResourceComposition resource1 = new ResourceComposition("Encounter/6", null);
        ResourceComposition resource2 = new ResourceComposition("Organization/7", null);
        setCompositionId(resource1, "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1");
        setCompositionId(resource2, "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1");

        when(resourceCompositionRepository.findById("Encounter/6")).thenReturn(Optional.of(resource1));
        when(resourceCompositionRepository.findById("Organization/7")).thenReturn(Optional.of(resource2));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compositionLookupProcessor.process(exchange);
        });

        assertEquals("The input resource or input bundle contains resource(s) that already exist in the database. No new resources detected.", exception.getMessage());
    }

    @Test
    void processWithSingleCompositionIdShouldThrowException() throws Exception {
        List<String> inputResourceIds = List.of("Encounter/6");
        exchange.setProperty(CamelConstants.INPUT_RESOURCE_IDS, inputResourceIds);
        exchange.setProperty(CamelConstants.INPUT_HTTP_METHOD, "POST");

        ResourceComposition resource1 = new ResourceComposition("Encounter/6", null);
        setCompositionId(resource1, "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1");

        when(resourceCompositionRepository.findById("Encounter/6")).thenReturn(Optional.of(resource1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compositionLookupProcessor.process(exchange);
        });

        assertEquals("The input resource or input bundle contains resource(s) that already exist in the database. No new resources detected.", exception.getMessage());
    }

    @Test
    void processWithNewResourceShouldNotThrowException() throws Exception {
        List<String> inputResourceIds = Arrays.asList("Encounter/6", "Organization/7", "NewResource/8");
        exchange.setProperty(CamelConstants.INPUT_RESOURCE_IDS, inputResourceIds);

        ResourceComposition resource1 = new ResourceComposition("Encounter/6", null);
        ResourceComposition resource2 = new ResourceComposition("Organization/7", null);
        ResourceComposition resource3 = new ResourceComposition("NewResource/8", null); // New resource without composition ID

        setCompositionId(resource1, "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1");
        setCompositionId(resource2, "07b59702-39e1-4a87-880c-6271fe66edea::local.ehrbase.org::1");

        when(resourceCompositionRepository.findById("Encounter/6")).thenReturn(Optional.of(resource1));
        when(resourceCompositionRepository.findById("Organization/7")).thenReturn(Optional.of(resource2));
        when(resourceCompositionRepository.findById("NewResource/8")).thenReturn(Optional.empty()); // New resource

        compositionLookupProcessor.process(exchange);
        verify(resourceCompositionRepository, times(1)).findById("Encounter/6");
        verify(resourceCompositionRepository, times(1)).findById("Organization/7");
        verify(resourceCompositionRepository, times(1)).findById("NewResource/8");
    }
}

