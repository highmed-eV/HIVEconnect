package org.highmed.hiveconnect;

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
@EnableJpaRepositories("org.highmed.hiveconnect.core.repository")
public class HiveConnectServerApplication {

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        SpringApplication.run(HiveConnectServerApplication.class, args);
    }
}
