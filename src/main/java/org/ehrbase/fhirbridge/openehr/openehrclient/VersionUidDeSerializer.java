package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class VersionUidDeSerializer extends StdDeserializer<VersionUid> {
    public VersionUidDeSerializer() {
        super(VersionUid.class);
    }

    @Override
    public VersionUid deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        return new VersionUid(jsonParser.getValueAsString());
    }
}

