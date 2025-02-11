package org.ehrbase.fhirbridge.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CamelRestAuthenticationConfig {

    @Bean
    @ConditionalOnProperty(name = "fhir-bridge.security.type", havingValue = "none")
    public Authenticator noAuthAuthenticator() {
        return (username, password) -> true; // No authentication
    }

    @Bean
    @ConditionalOnProperty(name = "fhir-bridge.security.type", havingValue = "basic")
    public Authenticator basicAuthAuthenticator() {
        return (username, password) -> 
            "user".equals(username) && "pass".equals(password);
    }

    @Bean
    @ConditionalOnProperty(name = "fhir-bridge.security.type", havingValue = "oauth2")
    public Authenticator oauth2Authenticator() {
        return (token, ignored) -> {
            // Validate the OAuth2 token
            return validateToken(token); 
        };
    }

    private boolean validateToken(String token) {
        // Implement token validation logic here
        return token != null && token.startsWith("Bearer ");
    }
}
