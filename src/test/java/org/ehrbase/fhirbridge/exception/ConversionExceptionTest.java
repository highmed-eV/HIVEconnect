package org.ehrbase.fhirbridge.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConversionExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Test conversion error";
        ConversionException exception = new ConversionException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Test conversion error";
        Throwable cause = new RuntimeException("Root cause");
        ConversionException exception = new ConversionException(message, cause);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
} 