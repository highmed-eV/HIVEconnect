package org.highmed.hiveconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FhirProperties.class)
@EnableConfigurationProperties(FhirProperties.class)
@TestPropertySource(properties = {
    "hive-connect.fhir.convert.auto-populate-display=true",
    "hive-connect.fhir.terminology-server.url=http://test-terminology-server"
})
class FhirPropertiesTest {

    @Autowired
    private FhirProperties fhirProperties;

    @Test
    void testFhirProperties() {
        assertNotNull(fhirProperties);
        assertNotNull(fhirProperties.getConvert());
        assertTrue(fhirProperties.getConvert().isAutoPopulateDisplay());
        assertNotNull(fhirProperties.getTerminologyServer());
        assertEquals("http://test-terminology-server", fhirProperties.getTerminologyServer().getUrl());
    }
} 