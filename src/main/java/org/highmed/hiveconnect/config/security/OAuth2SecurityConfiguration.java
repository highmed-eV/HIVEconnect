package org.highmed.hiveconnect.config.security;

import org.highmed.hiveconnect.security.SmartOnFhirAuthorizationInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@link Configuration} for Spring Security using OAuth2.
 *
 * @author Renaud Subiger
 * @since 1.6
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity 
@ConditionalOnProperty(value = "fhir-bridge.security.type", havingValue = "oauth2")
public class OAuth2SecurityConfiguration {

    private final SecurityProperties properties;

    public OAuth2SecurityConfiguration(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withJwkSetUri(properties.getOauth2().getJwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.from(properties.getOauth2().getJwsAlgorithm()))
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    @Bean
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> 
                    auth.anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .headers(headers -> headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .xssProtection(HeadersConfigurer.XXssConfig::disable)
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/fhir/**"))
                .build();
    }

    @Bean
    public SmartOnFhirAuthorizationInterceptor authorizationInterceptor() {
        return new SmartOnFhirAuthorizationInterceptor();
    }
}
