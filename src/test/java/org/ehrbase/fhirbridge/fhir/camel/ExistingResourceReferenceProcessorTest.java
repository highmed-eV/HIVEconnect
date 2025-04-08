package org.ehrbase.fhirbridge.fhir.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
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
        existingResourceReferenceProcessor = new ExistingResourceReferenceProcessor(objectMapper,resourceCompositionRepository);
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
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, inputResourceBundle);
        exchange.getIn().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, "systemId");

        when(resourceCompositionRepository.findInternalResourceIdByInputResourceIdAndSystemId("Patient/1", "systemId")).thenReturn("Patient/101");
        when(resourceCompositionRepository.findInternalResourceIdByInputResourceIdAndSystemId("Observation/2", "systemId")).thenReturn("Observation/102");

        existingResourceReferenceProcessor.process(exchange);

        // Verify the updated bundle
        Bundle updatedBundle = exchange.getIn().getBody(Bundle.class);
        assertEquals(2, updatedBundle.getEntry().size()); // 1 entry already present + 1 new entry for the existing resource
        boolean hasPatientWithId101 = updatedBundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource) 
            .filter(resource -> resource instanceof Patient) 
            .map(resource -> (Patient) resource) 
            .anyMatch(patient -> "101".equals(patient.getIdElement().getIdPart())); 

        assertTrue(hasPatientWithId101); // Check if the existing resource ID was replaced
    }

    @Test
    void processWithNoExistingResources() throws Exception {
        exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, null);

        String inputResourceBundle = "{ \"resourceType\": \"Bundle\", \"entry\": [ { \"resource\": { \"resourceType\": \"Patient\", \"id\": \"101\" } } ] }";
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, inputResourceBundle);

        existingResourceReferenceProcessor.process(exchange);

        // Verify no change in the bundle
        Bundle updatedBundle = exchange.getIn().getBody(Bundle.class);
        assertEquals(1, updatedBundle.getEntry().size()); // 1 entry already present + 1 new entry for the existing resource
        boolean hasPatientWithId101 = updatedBundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource) 
            .filter(resource -> resource instanceof Patient) 
            .map(resource -> (Patient) resource) 
            .anyMatch(patient -> "101".equals(patient.getIdElement().getIdPart())); 

        assertTrue(hasPatientWithId101); // Check if the existing resource ID was replaced
    }

    @Test
    void processWithInvalidBundle() throws Exception {
        // Prepare mock data with invalid input (not a Bundle)
        String invalidInputResourceBundle = "{ \"resourceType\": \"Patient\", \"id\": \"1\" }";
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, invalidInputResourceBundle);

        existingResourceReferenceProcessor.process(exchange);

        // No change expected
        assertNull(exchange.getIn().getBody());
    }
}

