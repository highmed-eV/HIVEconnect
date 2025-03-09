package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ServerPatientResourceProcessorTest {

    private ServerPatientResourceProcessor serverPatientResourceProcessor;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        serverPatientResourceProcessor = new ServerPatientResourceProcessor();
        DefaultCamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    void processWithValidPatientResource() throws Exception {
        Patient patientResource = new Patient();
        patientResource.setId("Patient/12345");
        exchange.getIn().setBody(patientResource);

        serverPatientResourceProcessor.process(exchange);

        Patient headerPatientResource = (Patient) exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE);
        assertNotNull(headerPatientResource);
        assertEquals("Patient/12345", headerPatientResource.getId());
    }

    @Test
    void processWithEmptyBody() throws Exception {
        exchange.getIn().setBody(null);

        serverPatientResourceProcessor.process(exchange);

        assertNull(exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE));
    }

    @Test
    void processWithInvalidBodyType() {
        exchange.getIn().setBody(new Object());

        assertThrows(ClassCastException.class, () -> serverPatientResourceProcessor.process(exchange));
    }
}

