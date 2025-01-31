package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.ehrbase.fhirbridge.openehr.openehrclient.CompositionFormat;
import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClientConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenEhrClientConfigTest {

    @Test
    void constructorAndGetBaseUri() {
        URI baseUri = URI.create("http://localhost:8080/ehrbase");
        OpenEhrClientConfig config = new OpenEhrClientConfig(baseUri);
        assertNotNull(config);
        assertEquals(baseUri, config.getBaseUri());
    }

    @Test
    void defaultCompositionFormat() {
        URI baseUri = URI.create("http://localhost:8080/ehrbase");
        OpenEhrClientConfig config = new OpenEhrClientConfig(baseUri);
        assertEquals(CompositionFormat.JSON, config.getCompositionFormat());
    }

    @Test
    void setCompositionFormat() {
        URI baseUri = URI.create("http://localhost:8080/ehrbase");
        OpenEhrClientConfig config = new OpenEhrClientConfig(baseUri);
        config.setCompositionFormat(CompositionFormat.XML);
        assertEquals(CompositionFormat.XML, config.getCompositionFormat());
    }
}

