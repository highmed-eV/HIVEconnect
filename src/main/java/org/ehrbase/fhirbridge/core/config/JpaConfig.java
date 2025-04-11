package org.ehrbase.fhirbridge.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaConfig {

    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(ZonedDateTime.now(ZoneOffset.UTC));
    }
} 