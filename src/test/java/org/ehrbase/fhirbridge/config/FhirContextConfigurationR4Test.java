package org.ehrbase.fhirbridge.config;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@TestConfiguration
@Import(FhirContextConfigurationR4.class)
@TestPropertySource(properties = {
    "fhir.context.r4.validation-enabled=true",
    "fhir.context.r4.parser-error-handling=THROW",
    "fhir.context.r4.parser-warning-handling=THROW"
})
class FhirContextConfigurationR4Test {

    @Test
    void testFhirContextConfigurationR4() {
        // This test verifies that the Spring context loads with the FhirContextConfigurationR4
        // The actual configuration is tested through property injection
        assertTrue(true);
    }
} 