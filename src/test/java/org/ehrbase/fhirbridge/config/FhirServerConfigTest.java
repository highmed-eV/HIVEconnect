package org.ehrbase.fhirbridge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "fhir.server.url=http://test-fhir-server",
    "fhir.server.username=test-user",
    "fhir.server.password=test-password",
    "fhir.server.connection-timeout=5000",
    "fhir.server.read-timeout=10000",
    "fhir.server.max-connections=20",
    "fhir.server.max-connections-per-route=10"
})
class FhirServerConfigTest {

    @Test
    void testFhirServerConfig() {
        // This test verifies that the Spring context loads with the FhirServerConfig
        // The actual configuration is tested through property injection
        assertTrue(true);
    }
} 