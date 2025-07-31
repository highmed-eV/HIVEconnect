package org.highmed.hiveconnect.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputOutputExceptionTest {

    @Test
    void testConstructor() {
        Class<?> entity = String.class;
        String entityId = "test-id";
        InputOutputException exception = new InputOutputException(entity, entityId);
        
        assertEquals(entity, exception.getEntity());
        assertEquals(entityId, exception.getEntityId());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }
} 