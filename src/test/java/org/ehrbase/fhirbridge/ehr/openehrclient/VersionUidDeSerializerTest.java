package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUidDeSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionUidDeSerializerTest {

    @Mock
    JsonParser jsonParser;

    @Mock
    DeserializationContext deserializationContext;

    @Test
    void deserialize() throws IOException {
        String versionString = "123e4567-e89b-12d3-a456-426614174000::system::42";
        when(jsonParser.getValueAsString()).thenReturn(versionString);

        VersionUidDeSerializer deserializer = new VersionUidDeSerializer();
        VersionUid result = deserializer.deserialize(jsonParser, deserializationContext);

        assertNotNull(result);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.getUuid().toString());
        assertEquals("system", result.getSystem());
        assertEquals(42L, result.getVersion());
        verify(jsonParser, times(1)).getValueAsString();
    }
}

