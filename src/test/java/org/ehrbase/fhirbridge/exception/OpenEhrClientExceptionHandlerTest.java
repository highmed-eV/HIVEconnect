package org.ehrbase.fhirbridge.exception;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.openehr.sdk.util.exception.OptimisticLockException;
import org.ehrbase.openehr.sdk.util.exception.WrongStatusCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class OpenEhrClientExceptionHandlerTest {

    private OpenEhrClientExceptionHandler handler;
    private Exchange exchange;

    @Mock
    private WrongStatusCodeException wrongStatusCodeException;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new OpenEhrClientExceptionHandler();
        exchange = new DefaultExchange(new DefaultCamelContext());
    }

    @Test
    void testHandleWrongStatusCode400() throws Exception {
        // Arrange
        String message = "Bad request";
        when(wrongStatusCodeException.getActualStatusCode()).thenReturn(400);
        when(wrongStatusCodeException.getMessage()).thenReturn(message);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, wrongStatusCodeException);

        // Act & Assert
        UnprocessableEntityException thrown = assertThrows(UnprocessableEntityException.class, () -> handler.process(exchange));
        assertEquals(message, thrown.getMessage());
    }

    @Test
    void testHandleOptimisticLock() throws Exception {
        // Arrange
        String message = "Version conflict";
        OptimisticLockException ex = new OptimisticLockException(message);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, ex);

        // Act & Assert
        ResourceVersionConflictException thrown = assertThrows(ResourceVersionConflictException.class, () -> handler.process(exchange));
        assertEquals(message, thrown.getMessage());
    }

    @Test
    void testHandleGenericException() throws Exception {
        // Arrange
        String message = "Generic error";
        Exception ex = new RuntimeException(message);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, ex);

        // Act & Assert
        InternalErrorException thrown = assertThrows(InternalErrorException.class, () -> handler.process(exchange));
        assertTrue(thrown.getMessage().contains("Error occurred while merging composition in EHRbase"));
        assertEquals(ex, thrown.getCause());
    }

    @Test
    void testProcessWithNullException() {
        // Arrange
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> handler.process(exchange));
    }
} 