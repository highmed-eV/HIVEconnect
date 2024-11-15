/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.fhirbridge.config;

import ca.uhn.fhir.context.FhirContext;
import org.apache.http.client.HttpClient;
import org.ehrbase.fhirbridge.service.TerminologyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FhirProperties.class)
@SuppressWarnings("java:S6212")
public class ConversionConfiguration {

//    @Bean(name = "fhirResourceConversionService")
//    public ConversionService conversionService() {
//        ConversionService conversionService = new ConversionService();
////        registerSampleConverter(conversionService);
//        return conversionService;
//    }

    @Bean
    @ConditionalOnProperty(prefix = "fhir-bridge.fhir.terminology-server", name = "url")
    public TerminologyService myTerminologyService(HttpClient httpClient, FhirProperties properties) {
        FhirContext context = FhirContext.forR4();
        context.getRestfulClientFactory().setHttpClient(httpClient);
        return new TerminologyService(context.newRestfulGenericClient(properties.getTerminologyServer().getUrl()));
    }

//    private void registerSampleConverter(ConversionService conversionService) {
//        conversionService.registerConverter(Profile.KDS_DIAGNOSE, new KDSDiagnoseCompositionConverter());
//    }

    //    @Bean(name = "fhirResourceConversionService")
//    public Object convert(Profile profile, Resource resource) {
//        KDSDiagnoseComposition composition;
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("flat_json/kds_diagnose_flat.json")) {
//            if (inputStream == null) {
//                throw new FileNotFoundException("File not found in resources");
//            }
//            composition = objectMapper.readValue(inputStream, KDSDiagnoseComposition.class);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return composition;
//    }

}