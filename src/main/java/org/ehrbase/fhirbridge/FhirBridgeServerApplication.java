package org.ehrbase.fhirbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Locale;

@SpringBootApplication
(exclude = {
    // ManagementWebSecurityAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
@EnableJpaRepositories("org.ehrbase.fhirbridge.core.repository")
public class FhirBridgeServerApplication {

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        SpringApplication.run(FhirBridgeServerApplication.class, args);
    }
}
