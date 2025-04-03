package org.ehrbase.fhirbridge.fhir.bundle;

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
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
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
class BundleResourceProviderTest {

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
    private BundleResourceProvider provider;

    private Bundle bundle;

    @BeforeEach
    void setUp() {
        bundle = new Bundle();
        bundle.setId("test-bundle-id");
        bundle.setType(Bundle.BundleType.TRANSACTION);
    }

    @Test
    void testCreateBundle() {
        // Arrange
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("POST"), eq(MethodOutcome.class)))
            .thenReturn(new MethodOutcome(new IdType("test-bundle-id")));

        // Act
        MethodOutcome outcome = provider.create(bundle, requestDetails, request, response);

        // Assert
        assertThat(outcome).isNotNull();
        assertThat(outcome.getId()).isNotNull();
        assertThat(outcome.getId().getValue()).isEqualTo("test-bundle-id");
        verify(producerTemplate).requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("POST"), eq(MethodOutcome.class));
    }

    @Test
    void testCreateBundleWithError() {
        // Arrange
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("POST"), eq(MethodOutcome.class)))
            .thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        assertThatThrownBy(() -> provider.create(bundle, requestDetails, request, response))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testUpdateBundle() {
        // Arrange
        IdType bundleId = new IdType("test-bundle-id");
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("PUT"), eq(MethodOutcome.class)))
            .thenReturn(new MethodOutcome(new IdType("test-bundle-id")));

        // Act
        MethodOutcome outcome = provider.update(bundleId, bundle, requestDetails, request, response);

        // Assert
        assertThat(outcome).isNotNull();
        assertThat(outcome.getId()).isNotNull();
        assertThat(outcome.getId().getValue()).isEqualTo("test-bundle-id");
        verify(producerTemplate).requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("PUT"), eq(MethodOutcome.class));
    }

    @Test
    void testUpdateBundleWithError() {
        // Arrange
        IdType bundleId = new IdType("test-bundle-id");
        when(producerTemplate.requestBodyAndHeader(anyString(), any(RequestDetails.class), eq(Exchange.HTTP_METHOD), eq("PUT"), eq(MethodOutcome.class)))
            .thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        assertThatThrownBy(() -> provider.update(bundleId, bundle, requestDetails, request, response))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testSearchBundle() {
        // Arrange
        when(producerTemplate.requestBody(anyString(), any(RequestDetails.class), eq(Bundle.class)))
            .thenReturn(bundle);

        // Act
        Bundle result = provider.searchBundle(
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
        assertThat(result.getId()).isEqualTo("test-bundle-id");
        assertThat(result.getType()).isEqualTo(Bundle.BundleType.TRANSACTION);
        verify(producerTemplate).requestBody(anyString(), any(RequestDetails.class), eq(Bundle.class));
    }

    @Test
    void testSearchBundleWithError() {
        // Arrange
        when(producerTemplate.requestBody(anyString(), any(RequestDetails.class), eq(Bundle.class)))
            .thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        assertThatThrownBy(() -> provider.searchBundle(
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
    void testReadBundle() {
        // Arrange
        IdType id = new IdType("test-bundle-id");
        when(producerTemplate.requestBody(anyString(), any(RequestDetails.class), eq(Bundle.class)))
            .thenReturn(bundle);

        // Act
        Bundle result = provider.readBundle(id, requestDetails, request, response);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-bundle-id");
        assertThat(result.getType()).isEqualTo(Bundle.BundleType.TRANSACTION);
        verify(producerTemplate).requestBody(anyString(), any(RequestDetails.class), eq(Bundle.class));
    }

    @Test
    void testReadBundleWithError() {
        // Arrange
        IdType id = new IdType("test-bundle-id");
        when(producerTemplate.requestBody(anyString(), any(RequestDetails.class), eq(Bundle.class)))
            .thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        assertThatThrownBy(() -> provider.readBundle(id, requestDetails, request, response))
            .isInstanceOf(RuntimeException.class);
    }
} 