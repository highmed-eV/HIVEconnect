package org.highmed.hiveconnect.exception;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenFHIRMappingExceptionHandlerTest {

    private OpenFHIRMappingExceptionHandler handler;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        handler = new OpenFHIRMappingExceptionHandler();
        exchange = new DefaultExchange(new DefaultCamelContext());
    }

    @Test
    void testProcessWithException() {
        // Arrange
        String message = "Test error";
        Exception ex = new RuntimeException(message);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, ex);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.process(exchange));
        assertTrue(thrown.getMessage().contains("Error occurred while openFHIR mapping conversion"));
        assertEquals(ex, thrown.getCause());
        assertEquals(message, exchange.getIn().getBody());
    }

    @Test
    void testProcessWithNullException() {
        // Arrange
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> handler.process(exchange));
    }
} 