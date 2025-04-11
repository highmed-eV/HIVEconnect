package org.ehrbase.fhirbridge.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = {CamelRestAuthenticationConfig.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "fhir-bridge.security.type=none")
class NoAuthCamelRestAuthenticationConfigTest {

    @Autowired
    private CamelRestAuthenticationConfig config;

    @Test
    void testNoAuthAuthenticator() {
        // Get the authenticator
        Authenticator authenticator = config.noAuthAuthenticator();
        
        // Test authentication
        assertTrue(authenticator.authenticate("any", "any"));
        assertTrue(authenticator.authenticate(null, null));
    }
}

@SpringBootTest(classes = {CamelRestAuthenticationConfig.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = {
    "fhir-bridge.security.type=basic",
    "fhir-bridge.security.user.name=test-user",
    "fhir-bridge.security.user.password=test-password"
})
class BasicAuthCamelRestAuthenticationConfigTest {

    @Autowired
    private CamelRestAuthenticationConfig config;

    @Test
    void testBasicAuthAuthenticator() {
        // Get the authenticator
        Authenticator authenticator = config.basicAuthAuthenticator();
        
        // Test valid credentials
        assertTrue(authenticator.authenticate("test-user", "test-password"));
        
        // Test invalid credentials
        assertFalse(authenticator.authenticate("wrong-user", "test-password"));
        assertFalse(authenticator.authenticate("test-user", "wrong-password"));
        assertFalse(authenticator.authenticate(null, null));
    }
}

@SpringBootTest(classes = {CamelRestAuthenticationConfig.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "fhir-bridge.security.type=oauth2")
class OAuth2CamelRestAuthenticationConfigTest {

    @Autowired
    private CamelRestAuthenticationConfig config;

    @Test
    void testOAuth2Authenticator() {
        // Get the authenticator
        Authenticator authenticator = config.oauth2Authenticator();
        
        // Test valid token
        assertTrue(authenticator.authenticate("Bearer valid-token", null));
        
        // Test invalid token
        assertFalse(authenticator.authenticate("invalid-token", null));
        assertFalse(authenticator.authenticate(null, null));
    }
} 