package org.highmed.hiveconnect.fhir.camel;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ExistingServerResourceProcessorTest {

    @Mock
    private FhirContext fhirContext;

    private ExistingServerResourceProcessor existingServerResourceProcessor;
    private Exchange exchange;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        existingServerResourceProcessor = new ExistingServerResourceProcessor(objectMapper);
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        fhirContext = FhirContext.forR4();
    }

    @Test
    void processWithValidResource() throws Exception {
        Patient patientResource = new Patient();
        patientResource.setId("Patient/12345");
        exchange.getIn().setBody(patientResource);

        List<String> existingResources = new ArrayList<>();
        exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, existingResources);
        existingServerResourceProcessor.process(exchange);
        existingResources = exchange.getProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, List.class);
        assertEquals(1, existingResources.size());

        String resourceJson = existingResources.get(0);
        assertTrue(resourceJson.contains("Patient"));
        assertTrue(resourceJson.contains("12345"));
    }

    @Test
    void processWithEmptyBodyShouldNotModifyExistingResources() throws Exception {
        exchange.getIn().setBody(null);

        List<String> existingResources = new ArrayList<>();
        exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, existingResources);

        existingServerResourceProcessor.process(exchange);

        assertTrue(existingResources.isEmpty());
    }
}

