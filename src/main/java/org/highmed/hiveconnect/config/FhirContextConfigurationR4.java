package org.highmed.hiveconnect.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPathEvaluationContext;
import ca.uhn.fhir.parser.JsonParser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.hapi.fluentpath.FhirPathR4;
import org.hl7.fhir.r4.model.Reference;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirContextConfigurationR4 {
    @Bean
    public FhirContext fhirContext() {
        return new FhirContext(FhirVersionEnum.R4);
    }

    @Bean
    public FhirPathR4 getFhirPath() {
        final FhirPathR4 fhirPathR4 = new FhirPathR4(FhirContext.forR4());
        fhirPathR4.setEvaluationContext(new IFhirPathEvaluationContext() {
            @Override
            public IBase resolveReference(@Nonnull IIdType theReference, @Nullable IBase theContext) {
                if(theContext == null) {
                    return null;
                }
                return ((Reference) theContext).getResource();
            }
        });
        return fhirPathR4;
    }


    @Bean
    public JsonParser getJsonParser() {
        return (JsonParser) FhirContext.forR4().newJsonParser();
    }

}
