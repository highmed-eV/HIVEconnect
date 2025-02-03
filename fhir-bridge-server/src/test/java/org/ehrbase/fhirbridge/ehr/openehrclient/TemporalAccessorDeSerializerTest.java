package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.ehrbase.fhirbridge.openehr.openehrclient.TemporalAccessorDeSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemporalAccessorDeSerializerTest {

    @Mock
    private TemporalAccessorDeSerializer deserializer;

    @Mock
    private JsonParser mockJsonParser;

    @Mock
    private DeserializationContext mockDeserializationContext;

    @BeforeEach
    void setUp() {
        deserializer = new TemporalAccessorDeSerializer();
    }

    @Test
    void deserializeWithDateTime() throws IOException {
        String dateTime = "2023-12-31T23:59:59";
        when(mockJsonParser.getValueAsString()).thenReturn(dateTime);

        TemporalAccessor result = deserializer.deserialize(mockJsonParser, mockDeserializationContext);

        assertNotNull(result);
        assertTrue(result instanceof LocalDateTime);
        assertEquals(LocalDateTime.parse(dateTime), result);
    }

    @Test
    void deserializeWithDate() throws IOException {
        String date = "2023-12-31";
        when(mockJsonParser.getValueAsString()).thenReturn(date);

        TemporalAccessor result = deserializer.deserialize(mockJsonParser, mockDeserializationContext);

        assertNotNull(result);
        assertTrue(result instanceof LocalDate);
        assertEquals(LocalDate.parse(date), result);
    }

    @Test
    void deserializeWithTime() throws IOException {
        String time = "23:59:59";
        when(mockJsonParser.getValueAsString()).thenReturn(time);

        TemporalAccessor result = deserializer.deserialize(mockJsonParser, mockDeserializationContext);

        assertNotNull(result);
        assertTrue(result instanceof LocalTime);
        assertEquals(LocalTime.parse(time), result);
    }

    @Test
    void deserializeWithInvalidFormat() throws IOException {
        String invalidValue = "invalid-date-time";
        when(mockJsonParser.getValueAsString()).thenReturn(invalidValue);

        assertThrows(RuntimeException.class, () -> deserializer.deserialize(mockJsonParser, mockDeserializationContext));
    }
}

