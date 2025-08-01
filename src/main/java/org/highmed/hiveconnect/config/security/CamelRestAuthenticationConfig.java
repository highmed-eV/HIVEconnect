package org.highmed.hiveconnect.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelRestAuthenticationConfig {

    private final SecurityProperties properties;

    public CamelRestAuthenticationConfig(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(name = "hive-connect.security.type", havingValue = "none")
    public Authenticator noAuthAuthenticator() {
        return (username, password) -> true; // No authentication
    }

    @Bean
    @ConditionalOnProperty(name = "hive-connect.security.type", havingValue = "basic")
    public Authenticator basicAuthAuthenticator() {
        return (username, password) -> 
            properties.getUser().getName().equals(username) && 
            properties.getUser().getPassword().equals(password);
    }

    @Bean
    @ConditionalOnProperty(name = "hive-connect.security.type", havingValue = "oauth2")
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
