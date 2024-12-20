package org.ehrbase.fhirbridge.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
// import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.SecurityFilterChain;


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
