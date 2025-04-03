package org.ehrbase.fhirbridge.config;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpInterceptorTest {

    private HttpInterceptor interceptor;
    
    @Mock
    private RequestDetails requestDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new HttpInterceptor();
    }

    @Test
    void testPreHandle_SetsTenantContext() {
        // Arrange
        when(requestDetails.getHeader("X-TenantId")).thenReturn("test-tenant");
        when(requestDetails.getHeader("X-OrganizationId")).thenReturn("test-org");
        when(requestDetails.getHeader("X-Auth-Request-Preferred-Username")).thenReturn("test-user");
        when(requestDetails.getRequestPath()).thenReturn("/test/path");

        // Act
        interceptor.preHandle(requestDetails);

        // Assert
        assertEquals("test-tenant", MDC.get("tenantId"));
        assertEquals("test-tenant", TenantContext.getTenantId());
        assertEquals("test-org", TenantContext.getOrganizationId());
        assertEquals("test-user", TenantContext.getUserName());
    }

    @Test
    void testPreHandle_WithNullHeaders() {
        // Arrange
        when(requestDetails.getHeader("X-TenantId")).thenReturn(null);
        when(requestDetails.getHeader("X-OrganizationId")).thenReturn(null);
        when(requestDetails.getHeader("X-Auth-Request-Preferred-Username")).thenReturn(null);
        when(requestDetails.getRequestPath()).thenReturn("/test/path");

        // Act
        interceptor.preHandle(requestDetails);

        // Assert
        assertEquals("0", MDC.get("tenantId"));
        assertEquals("0", TenantContext.getTenantId());
        assertEquals("0", TenantContext.getOrganizationId());
        assertEquals("anonymous", TenantContext.getUserName());
    }

    @Test
    void testAfterCompletion_ClearsContext() {
        // Arrange
        TenantContext.setContext(requestDetails);

        // Act
        interceptor.afterCompletion(requestDetails);

        // Assert
        assertNull(MDC.get("tenantId"));
        assertEquals("0", TenantContext.getTenantId());
        assertEquals("0", TenantContext.getOrganizationId());
        assertEquals("anonymous", TenantContext.getUserName());
    }
} 