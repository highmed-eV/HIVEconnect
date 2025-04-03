package org.ehrbase.fhirbridge.fhir.processor;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.fhir.camel.ExistingServerPatientResourceProcessor;
import org.ehrbase.fhirbridge.fhir.camel.ExistingServerResourceProcessor;
import org.ehrbase.fhirbridge.fhir.camel.ServerPatientResourceProcessor;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FhirProcessorTest {

    private Exchange exchange;

    private ServerPatientResourceProcessor serverPatientResourceProcessor;
    private ExistingServerResourceProcessor existingServerResourceProcessor;
    private ExistingServerPatientResourceProcessor existingServerPatientResourceProcessor;

    @BeforeEach
    void setUp() {
        exchange = new DefaultExchange(new DefaultCamelContext());
        
        serverPatientResourceProcessor = new ServerPatientResourceProcessor();
        existingServerResourceProcessor = new ExistingServerResourceProcessor();
        existingServerPatientResourceProcessor = new ExistingServerPatientResourceProcessor();
    }

    @Test
    void testServerPatientResourceProcessor_WithValidPatient() throws Exception {
        // Arrange
        Patient patient = new Patient();
        patient.setId("test-patient-id");
        exchange.getIn().setBody(patient);

        // Act
        serverPatientResourceProcessor.process(exchange);

        // Assert
        assertEquals(patient, exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
    }

    @Test
    void testServerPatientResourceProcessor_WithEmptyBody() throws Exception {
        // Arrange
        exchange.getIn().setBody(null);

        // Act
        serverPatientResourceProcessor.process(exchange);

        // Assert
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
    }

    @Test
    void testExistingServerResourceProcessor_WithValidResource() throws Exception {
        // Arrange
        Resource resource = new Patient();
        resource.setId("test-resource-id");
        List<String> existingResources = new ArrayList<>();
        
        exchange.getIn().setBody(resource);
        exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, existingResources);

        // Act
        existingServerResourceProcessor.process(exchange);

        // Assert
        List<String> updatedResources = exchange.getProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, List.class);
        assertEquals(1, updatedResources.size());
        assertEquals("{\"resourceType\":\"Patient\",\"id\":\"test-resource-id\"}", updatedResources.get(0));
    }

    @Test
    void testExistingServerResourceProcessor_WithEmptyBody() throws Exception {
        // Arrange
        exchange.getIn().setBody(null);

        // Act
        existingServerResourceProcessor.process(exchange);

        // Assert
        assertNull(exchange.getProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES));
    }

    @Test
    void testExistingServerPatientResourceProcessor_WithValidPatient() throws Exception {
        // Arrange
        Patient patient = new Patient();
        patient.setId("test-patient-id");
        exchange.getIn().setBody(patient);

        // Act
        existingServerPatientResourceProcessor.process(exchange);

        // Assert
        assertEquals(patient, exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
        assertEquals("test-patient-id", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void testExistingServerPatientResourceProcessor_WithSearchUrlAndEmptyBody() throws Exception {
        // Arrange
        exchange.getIn().setBody(null);
        exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "SEARCH_URL");
        exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_SEARCH_URL, "test-search-url");

        // Act & Assert
        assertThrows(UnprocessableEntityException.class, () -> 
            existingServerPatientResourceProcessor.process(exchange));
    }

    @Test
    void testExistingServerPatientResourceProcessor_WithRelativeReference() throws Exception {
        // Arrange
        exchange.getIn().setBody(null);
        exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "RELATIVE_REFERENCE");

        // Act
        existingServerPatientResourceProcessor.process(exchange);

        // Assert
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }
} 