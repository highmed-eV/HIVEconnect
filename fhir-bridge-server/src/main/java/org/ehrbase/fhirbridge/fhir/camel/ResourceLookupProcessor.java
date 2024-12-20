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

 package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link org.apache.camel.Processor Processor} that retrieves the internalResourceId corresponding to the inputResourceId involved in the current
 * exchange. If the resourceId is present in the db return the corresponding internalResourceIds.
 *
 * @since 1.2.0
 */
@Component(ResourceLookupProcessor.BEAN_ID)
@SuppressWarnings("java:S6212")
public class ResourceLookupProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "resourceLookupProcessor";

    private static final Logger LOG = LoggerFactory.getLogger(ResourceLookupProcessor.class);

    private final ResourceCompositionRepository resourceCompositionRepository;

    public ResourceLookupProcessor(ResourceCompositionRepository resourceCompositionRepository) {
        this.resourceCompositionRepository = resourceCompositionRepository;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        List<String > inputResourceIds = exchange.getProperty(CamelConstants.INPUT_RESOURCE_IDS, List.class);
        List<String> internalResourceIds = new ArrayList<>();

        for (String inputResourceId : inputResourceIds){
            Optional<String> optionalInternalResourceId = resourceCompositionRepository.findById(inputResourceId)
                    .map(ResourceComposition::getInternalResourceId);

            if (optionalInternalResourceId.isPresent()) {
                String internalResourceId = optionalInternalResourceId.get();
                internalResourceIds.add(internalResourceId);
            }
        }
        exchange.setProperty(CamelConstants.INTERNAL_RESOURCE_IDS, internalResourceIds);
//        exchange.getMessage().setHeader(CamelConstants.INTERNAL_RESOURCE_IDS, internalResourceIds);
    }
}
