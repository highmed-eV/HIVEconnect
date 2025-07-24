package org.highmed.hiveconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CorsProperties.class)
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = {
    "fhir-bridge.cors.allowed-origins=http://localhost:8080,http://localhost:3000",
    "fhir-bridge.cors.allowed-methods=GET,POST,PUT,DELETE",
    "fhir-bridge.cors.allowed-headers=Authorization,Content-Type",
    "fhir-bridge.cors.allow-credentials=true"
})
class CorsPropertiesTest {

    @Autowired
    private CorsProperties corsProperties;

    @Test
    void testCorsProperties() {
        assertNotNull(corsProperties);
        
        List<String> expectedOrigins = Arrays.asList("http://localhost:8080", "http://localhost:3000");
        assertEquals(expectedOrigins, corsProperties.getAllowedOrigins());
        
        List<String> expectedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE");
        assertEquals(expectedMethods, corsProperties.getAllowedMethods());
        
        List<String> expectedHeaders = Arrays.asList("Authorization", "Content-Type");
        assertEquals(expectedHeaders, corsProperties.getAllowedHeaders());
        
        assertTrue(corsProperties.isAllowCredentials());
    }
} 