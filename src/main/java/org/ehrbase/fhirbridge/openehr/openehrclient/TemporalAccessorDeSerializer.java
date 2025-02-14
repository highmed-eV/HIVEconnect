package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nedap.archie.datetime.DateTimeParsers;

import java.io.IOException;
import java.time.temporal.TemporalAccessor;

public class TemporalAccessorDeSerializer extends StdDeserializer<TemporalAccessor> {

    public TemporalAccessorDeSerializer() {
        super(TemporalAccessor.class);
    }

    @Override
    public TemporalAccessor deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        String value = jsonParser.getValueAsString();
        try {
            return DateTimeParsers.parseTimeValue(value);
        } catch (RuntimeException e1) {
            try {
                return DateTimeParsers.parseDateValue(value);
            } catch (RuntimeException e2) {
                return DateTimeParsers.parseDateTimeValue(value);
            }
        }
    }
}
