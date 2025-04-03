package org.ehrbase.fhirbridge.config.openehr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OpenEhrPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
    "fhir-bridge.openehr.url=https://test-openehr-server.com",
    "fhir-bridge.openehr.update-templates-on-startup=true",
    "fhir-bridge.openehr.security.type=BASIC",
    "fhir-bridge.openehr.security.user.name=admin",
    "fhir-bridge.openehr.security.user.password=secret",
    "fhir-bridge.openehr.security.oauth2.token-url=https://auth-server.com/token",
    "fhir-bridge.openehr.security.oauth2.client-id=client",
    "fhir-bridge.openehr.security.oauth2.client-secret=secret"
})
class OpenEhrPropertiesTest {

    @EnableConfigurationProperties(OpenEhrProperties.class)
    static class TestConfig {
        // Empty config just to enable properties
    }

    @Autowired
    private OpenEhrProperties properties;

    @Test
    void shouldBindTopLevelProperties() {
        assertThat(properties.getUrl()).isEqualTo("https://test-openehr-server.com");
        assertThat(properties.isUpdateTemplatesOnStartup()).isTrue();
    }

    @Test
    void shouldBindSecurityProperties() {
        OpenEhrProperties.Security security = properties.getSecurity();
        assertThat(security.getType()).isEqualTo(OpenEhrProperties.SecurityType.BASIC);
    }

    @Test
    void shouldBindUserCredentials() {
        OpenEhrProperties.User user = properties.getSecurity().getUser();
        assertThat(user.getName()).isEqualTo("admin");
        assertThat(user.getPassword()).isEqualTo("secret");
    }

    @Test
    void shouldBindOAuth2Properties() {
        OpenEhrProperties.OAuth2 oauth2 = properties.getSecurity().getOauth2();
        assertThat(oauth2.getTokenUrl()).isEqualTo("https://auth-server.com/token");
        assertThat(oauth2.getClientId()).isEqualTo("client");
        assertThat(oauth2.getClientSecret()).isEqualTo("secret");
    }

    @Test
    void shouldHandleDefaultValues() {
        // Test with empty properties to verify defaults
        assertThat(new OpenEhrProperties().getSecurity().getType())
            .isEqualTo(OpenEhrProperties.SecurityType.NONE);
    }

    @Test
    void shouldSupportAllSecurityTypes() {
        // Verify enum values are properly bound
        assertThat(OpenEhrProperties.SecurityType.values())
            .containsExactly(
                OpenEhrProperties.SecurityType.NONE,
                OpenEhrProperties.SecurityType.BASIC,
                OpenEhrProperties.SecurityType.OAUTH2
            );
    }
}