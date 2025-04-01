package org.ehrbase.fhirbridge.fhir.patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.HumanName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientResourceProviderTest {

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private RequestDetails requestDetails;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private PatientResourceProvider provider;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId("test-patient-id");
        
        // Add identifier
        Identifier identifier = new Identifier();
        identifier.setSystem("http://test-system.com");
        identifier.setValue("test-identifier");
        patient.addIdentifier(identifier);
        
        // Add name
        HumanName name = new HumanName();
        name.setFamily("Test");
        name.addGiven("Patient");
        patient.addName(name);
    }

    @Test
    void testCreatePatient() {
        // Arrange
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("POST"), eq(MethodOutcome.class)))
            .thenReturn(new MethodOutcome(new IdType("test-patient-id")));

        // Act
        MethodOutcome outcome = provider.create(patient, requestDetails, request, response);

        // Assert
        assertThat(outcome).isNotNull();
        assertThat(outcome.getId()).isNotNull();
        assertThat(outcome.getId().getValue()).isEqualTo("test-patient-id");
        verify(producerTemplate).requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("POST"), eq(MethodOutcome.class));
    }

    @Test
    void testCreatePatientWithError() {
        // Arrange
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("POST"), eq(MethodOutcome.class)))
            .thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        assertThatThrownBy(() -> provider.create(patient, requestDetails, request, response))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testSearchPatient() {
        // Arrange
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("GET"), eq(Patient.class)))
            .thenReturn(patient);

        // Act
        Patient result = provider.searchPatient(
            null, // id
            null, // language
            null, // lastUpdated
            null, // profile
            null, // source
            null, // security
            null, // tag
            null, // content
            null, // text
            null, // filter
            null, // identifier
            null, // count
            null, // offset
            null, // sort
            requestDetails,
            request,
            response
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-patient-id");
        assertThat(result.getIdentifier()).hasSize(1);
        assertThat(result.getIdentifierFirstRep().getValue()).isEqualTo("test-identifier");
        assertThat(result.getName()).hasSize(1);
        assertThat(result.getNameFirstRep().getFamily()).isEqualTo("Test");
        verify(producerTemplate).requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("GET"), eq(Patient.class));
    }

    @Test
    void testSearchPatientWithError() {
        // Arrange
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("GET"), eq(Patient.class)))
            .thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        assertThatThrownBy(() -> provider.searchPatient(
            null, // id
            null, // language
            null, // lastUpdated
            null, // profile
            null, // source
            null, // security
            null, // tag
            null, // content
            null, // text
            null, // filter
            null, // identifier
            null, // count
            null, // offset
            null, // sort
            requestDetails,
            request,
            response
        )).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testReadPatient() {
        // Arrange
        IdType id = new IdType("test-patient-id");
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("GET"), eq(Patient.class)))
            .thenReturn(patient);

        // Act
        Patient result = provider.readPatient(id, requestDetails, request, response);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-patient-id");
        assertThat(result.getIdentifier()).hasSize(1);
        assertThat(result.getIdentifierFirstRep().getValue()).isEqualTo("test-identifier");
        assertThat(result.getName()).hasSize(1);
        assertThat(result.getNameFirstRep().getFamily()).isEqualTo("Test");
        verify(producerTemplate).requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("GET"), eq(Patient.class));
    }

    @Test
    void testReadPatientWithError() {
        // Arrange
        IdType id = new IdType("test-patient-id");
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("GET"), eq(Patient.class)))
            .thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        assertThatThrownBy(() -> provider.readPatient(id, requestDetails, request, response))
            .isInstanceOf(RuntimeException.class);
    }
} 