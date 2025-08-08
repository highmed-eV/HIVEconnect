package org.highmed.hiveconnect.fhir.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExistingServerPatientResourceProcessorTest {

    private ExistingServerPatientResourceProcessor existingServerPatientResourceProcessor;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        existingServerPatientResourceProcessor = new ExistingServerPatientResourceProcessor();
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithValidPatient() {
        Patient patientResource = new Patient();
        patientResource.setId("Patient/12345");
        exchange.getIn().setBody(patientResource);

        assertDoesNotThrow(() -> existingServerPatientResourceProcessor.process(exchange));

        assertEquals(patientResource, exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
        assertEquals("Patient/12345", exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void processWithEmptyBodyShouldNotSetHeaders() {
        exchange.getIn().setBody(null);
        exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE, "SEARCH_URL");
        assertThrows(UnprocessableEntityException.class, () -> existingServerPatientResourceProcessor.process(exchange));

        
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID));
    }

    @Test
    void processWithInvalidBodyShouldThrowException() {
        exchange.getIn().setBody(new Object());

        assertThrows(ClassCastException.class, () -> existingServerPatientResourceProcessor.process(exchange));
    }
}

