package org.ehrbase.fhirbridge.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirContextConfigurationR4 {
    @Bean
    public FhirContext fhirContext() {
        return new FhirContext(FhirVersionEnum.R4);
    }

}
