package org.ehrbase.fhirbridge.fhir.processor;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.fhir.camel.RequestDetailsLookupProcessor;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestDetailsLookupProcessorTest {

    private Exchange exchange;
    private MockHttpServletRequest request;

    @Mock
    private ServletRequestDetails requestDetails;

    private RequestDetailsLookupProcessor requestDetailsLookupProcessor;

    @BeforeEach
    void setUp() {
        exchange = new DefaultExchange(new DefaultCamelContext());
        requestDetailsLookupProcessor = new RequestDetailsLookupProcessor();
        request = new MockHttpServletRequest();
    }

    @Test
    void testProcess_WithValidRequestDetails() throws Exception {
        // Arrange
        exchange.getIn().setBody(requestDetails);
        when(requestDetails.getServletRequest()).thenReturn(request);
        when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
        when(requestDetails.getResourceName()).thenReturn("Patient");
        when(requestDetails.getRequestPath()).thenReturn("/Patient");
        when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.CREATE);
        when(requestDetails.getResource()).thenReturn(new Patient());
        when(requestDetails.getId()).thenReturn(new IdType("Patient/123"));
        request.addHeader("X-System-ID", "test-system");

        // Act
        requestDetailsLookupProcessor.process(exchange);

        // Assert
        assertNotNull(exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE));
        assertNotNull(exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE));
        assertEquals("CREATE", exchange.getIn().getHeader(CamelConstants.REQUESTDETAILS_OPERATION_TYPE));
        assertEquals("POST", exchange.getIn().getHeader(CamelConstants.REQUEST_HTTP_METHOD));
        assertEquals("Patient", exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE));
        assertEquals("test-system", exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID));
        assertEquals("123", exchange.getIn().getHeader(CamelConstants.REQUESTDETAILS_ID));
    }

    @Test
    void testProcess_WithNullRequestDetails() throws Exception {
        // Arrange
        exchange.getIn().setBody(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            requestDetailsLookupProcessor.process(exchange));
    }

    @Test
    void testProcess_WithNullResourceForNonGetRequest() throws Exception {
        // Arrange
        exchange.getIn().setBody(requestDetails);
        when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
        when(requestDetails.getResourceName()).thenReturn("Patient");
        when(requestDetails.getRequestPath()).thenReturn("/Patient");
        when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.CREATE);
        when(requestDetails.getResource()).thenReturn(null);
        when(requestDetails.getId()).thenReturn(new IdType("Patient/123"));
        request.addHeader("X-System-ID", "test-system");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            requestDetailsLookupProcessor.process(exchange));
    }

    @Test
    void testProcess_WithNullResourceForGetRequest() throws Exception {
        // Arrange
        exchange.getIn().setBody(requestDetails);
        when(requestDetails.getServletRequest()).thenReturn(request);
        when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);
        when(requestDetails.getResourceName()).thenReturn("Patient");
        when(requestDetails.getRequestPath()).thenReturn("/Patient");
        when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.READ);
        when(requestDetails.getId()).thenReturn(new IdType("Patient/123"));
        request.addHeader("X-System-ID", "test-system");

        // Act
        requestDetailsLookupProcessor.process(exchange);

        // Assert
        assertNull(exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE));
        assertEquals("test-system", exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID));
        assertEquals("123", exchange.getIn().getHeader(CamelConstants.REQUESTDETAILS_ID));
    }

    @Test
    void testProcess_WithDifferentOperationType() throws Exception {
        // Arrange
        exchange.getIn().setBody(requestDetails);
        when(requestDetails.getServletRequest()).thenReturn(request);
        when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
        when(requestDetails.getResourceName()).thenReturn("Patient");
        when(requestDetails.getRequestPath()).thenReturn("/Patient");
        when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
        when(requestDetails.getResource()).thenReturn(new Patient());
        when(requestDetails.getId()).thenReturn(new IdType("Patient/123"));
        request.addHeader("X-System-ID", "test-system");

        // Act
        requestDetailsLookupProcessor.process(exchange);

        // Assert
        assertEquals("Patient", exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE));
        assertEquals("SEARCH", exchange.getIn().getHeader(CamelConstants.REQUESTDETAILS_OPERATION_TYPE));
        assertEquals("test-system", exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID));
        assertEquals("123", exchange.getIn().getHeader(CamelConstants.REQUESTDETAILS_ID));
    }

    @Test
    void testProcess_WithDefaultSystemId() throws Exception {
        // Arrange
        exchange.getIn().setBody(requestDetails);
        when(requestDetails.getServletRequest()).thenReturn(request);
        when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
        when(requestDetails.getResourceName()).thenReturn("Patient");
        when(requestDetails.getRequestPath()).thenReturn("/Patient");
        when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.CREATE);
        when(requestDetails.getResource()).thenReturn(new Patient());
        when(requestDetails.getId()).thenReturn(new IdType("Patient/123"));
        request.addHeader("Authorization", "Bearer test-token");

        // Act
        requestDetailsLookupProcessor.process(exchange);

        // Assert
        assertEquals("default-system", exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID));
        assertEquals("123", exchange.getIn().getHeader(CamelConstants.REQUESTDETAILS_ID));
    }

    @Test
    void testProcess_WithUnknownSystemId() throws Exception {
        // Arrange
        exchange.getIn().setBody(requestDetails);
        when(requestDetails.getServletRequest()).thenReturn(request);
        when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
        when(requestDetails.getResourceName()).thenReturn("Patient");
        when(requestDetails.getRequestPath()).thenReturn("/Patient");
        when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.CREATE);
        when(requestDetails.getResource()).thenReturn(new Patient());
        when(requestDetails.getId()).thenReturn(new IdType("Patient/123"));

        // Act
        requestDetailsLookupProcessor.process(exchange);

        // Assert
        assertEquals("unknown-system", exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID));
        assertEquals("123", exchange.getIn().getHeader(CamelConstants.REQUESTDETAILS_ID));
    }

    @Test
    void testGetRequestDetails_WithValidRequestDetails() {
        // Arrange
        exchange.getIn().setBody(requestDetails);

        // Act
        ServletRequestDetails result = exchange.getIn().getBody(ServletRequestDetails.class);

        // Assert
        assertNotNull(result);
        assertEquals(requestDetails, result);
    }

    @Test
    void testGetRequestDetails_WithNullRequestDetails() {
        // Arrange
        exchange.getIn().setBody(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            requestDetailsLookupProcessor.process(exchange));
    }
} 