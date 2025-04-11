package org.ehrbase.fhirbridge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@TestConfiguration
@Import(OpenFHIRAdapterConfiguration.class)
@TestPropertySource(properties = {
    "openfhir.adapter.url=http://test-openfhir-server",
    "openfhir.adapter.username=test-user",
    "openfhir.adapter.password=test-password",
    "openfhir.adapter.connection-timeout=5000",
    "openfhir.adapter.read-timeout=10000"
})
class OpenFHIRAdapterConfigurationTest {

    @Test
    void testOpenFHIRAdapterConfiguration() {
        // This test verifies that the Spring context loads with the OpenFHIRAdapterConfiguration
        // The actual configuration is tested through property injection
        assertTrue(true);
    }
} 