package org.ehrbase.fhirbridge.ehr.camel;

import com.nedap.archie.rm.ehr.EhrStatus;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.ehrbase.fhirbridge.openehr.camel.EhrLookupProcessor;
// import org.ehrbase.fhirbridge.openehr.openehrclient.AqlEndpoint;
// import org.ehrbase.fhirbridge.openehr.openehrclient.EhrEndpoint;
// import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClient;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClient;
import org.ehrbase.openehr.sdk.client.openehrclient.AqlEndpoint;
import org.ehrbase.openehr.sdk.client.openehrclient.EhrEndpoint;

import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EhrLookupProcessorTests {

    @Mock
    private PatientEhrRepository patientEhrRepository;

    @Mock
    private OpenEhrClient openEhrClient;

    @Mock
    private AqlEndpoint aqlEndpoint;

    @Mock
    private EhrEndpoint ehrEndpoint;

    @Mock
    private EhrLookupProcessor ehrLookupProcessor;

    private Exchange exchange;

    @BeforeEach
    void setUp() {
        ehrLookupProcessor = new EhrLookupProcessor(patientEhrRepository, openEhrClient);
        exchange = createExchange(new Patient());
    }

    @Test
    void processWithExistingEhrId() throws Exception {
        UUID existingEhrId = UUID.randomUUID();
        PatientEhr mockPatientEhr = new PatientEhr("http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym|123", "Patient/123", "system123", existingEhrId);
        when(patientEhrRepository.findByInternalPatientIdAndSystemId(anyString(), anyString())).thenReturn(mockPatientEhr);

        ehrLookupProcessor.process(exchange);

        UUID ehrId = exchange.getMessage().getHeader(CompositionConstants.EHR_ID, UUID.class);
        assertEquals(existingEhrId, ehrId);
        verify(patientEhrRepository, times(1)).findByInternalPatientIdAndSystemId(anyString(), anyString());
    }

    @Test
    void processWithNewEhrId() throws Exception {
        when(patientEhrRepository.findByInternalPatientIdAndSystemId(anyString(), anyString())).thenReturn(null);

        when(openEhrClient.aqlEndpoint()).thenReturn(aqlEndpoint);
        when(openEhrClient.aqlEndpoint().execute(any())).thenReturn(List.of());
        when(openEhrClient.ehrEndpoint()).thenReturn(ehrEndpoint);
        when(ehrEndpoint.createEhr(any(EhrStatus.class))).thenReturn(UUID.randomUUID());

        ehrLookupProcessor.process(exchange);

        UUID ehrId = exchange.getMessage().getHeader(CompositionConstants.EHR_ID, UUID.class);
        assertNotNull(ehrId);
        verify(patientEhrRepository, times(1)).save(any(PatientEhr.class));
    }

    private Exchange createExchange(Patient patient) {
        DefaultCamelContext camelContext = new DefaultCamelContext();
        Exchange defaultExchange = new DefaultExchange(camelContext);
        patient.addIdentifier()
                .setSystem("http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym")
                .setValue("123");
        defaultExchange.getMessage().setHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE, patient);
        defaultExchange.getMessage().setHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID, "system123");
        defaultExchange.getMessage().setHeader(CamelConstants.FHIR_INPUT_PATIENT_ID, "http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym|123");
        defaultExchange.getMessage().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, "Patient/123");
        return defaultExchange;
    }

    @Test
    void extractPatientId() {
        String patientIdStr = "/fhir/Patient/1234";
        String result = ehrLookupProcessor.extractPatientId(patientIdStr);
        assertEquals("Patient/1234", result);
    }

    @Test
    void extractPatientIdWithInvalidFormat() {
        String patientIdStr = "Invalid/Patient/1234";
        String result = ehrLookupProcessor.extractPatientId(patientIdStr);
        assertEquals(patientIdStr, result);
    }
}
