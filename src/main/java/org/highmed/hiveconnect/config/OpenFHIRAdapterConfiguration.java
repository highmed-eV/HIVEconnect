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

package org.highmed.hiveconnect.config;

import org.highmed.hiveconnect.openehr.DefaultTemplateProvider;
import org.highmed.hiveconnect.openfhir.openfhirclient.OpenFHIRAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@SuppressWarnings("java:S6212")
public class OpenFHIRAdapterConfiguration {
    private final RestTemplate restTemplate;
    private final DefaultTemplateProvider templateProvider;

    public OpenFHIRAdapterConfiguration(RestTemplate restTemplate, DefaultTemplateProvider templateProvider) {
        this.restTemplate = restTemplate;
        this.templateProvider = templateProvider;
    }

    @Bean(name = "hiveConnectOpenFHIRAdapter")
    public OpenFHIRAdapter openFHIRAdapter() {
        return new OpenFHIRAdapter(restTemplate, templateProvider);
    }
}