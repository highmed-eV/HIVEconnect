package org.highmed.hiveconnect.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SecurityProperties.class)
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = {
    "fhir-bridge.security.type=BASIC",
    "fhir-bridge.security.user.name=test-user",
    "fhir-bridge.security.user.password=test-password",
    "fhir-bridge.security.oauth2.jwk-set-uri=http://test-jwk-uri",
    "fhir-bridge.security.oauth2.jws-algorithm=RS256"
})
class SecurityPropertiesTest {

    @Autowired
    private SecurityProperties securityProperties;

    @Test
    void testSecurityProperties() {
        assertNotNull(securityProperties);
        
        // Test security type
        assertEquals(SecurityProperties.SecurityType.BASIC, securityProperties.getType());
        
        // Test user properties
        SecurityProperties.User user = securityProperties.getUser();
        assertNotNull(user);
        assertEquals("test-user", user.getName());
        assertEquals("test-password", user.getPassword());
        
        // Test OAuth2 properties
        SecurityProperties.OAuth2 oauth2 = securityProperties.getOauth2();
        assertNotNull(oauth2);
        assertEquals("http://test-jwk-uri", oauth2.getJwkSetUri());
        assertEquals("RS256", oauth2.getJwsAlgorithm());
    }

    @Test
    void testSecurityTypeEnum() {
        SecurityProperties.SecurityType[] types = SecurityProperties.SecurityType.values();
        assertEquals(3, types.length);
        assertArrayEquals(new SecurityProperties.SecurityType[]{
            SecurityProperties.SecurityType.NONE,
            SecurityProperties.SecurityType.BASIC,
            SecurityProperties.SecurityType.OAUTH2
        }, types);
    }
} 