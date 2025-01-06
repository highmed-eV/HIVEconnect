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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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

    @Autowired
    private final ResourceCompositionRepository resourceCompositionRepository;

    public ResourceLookupProcessor(ResourceCompositionRepository resourceCompositionRepository) {
        this.resourceCompositionRepository = resourceCompositionRepository;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        List<String > inputResourceIds = exchange.getProperty(CamelConstants.REFERENCE_INPUT_RESOURCE_IDS, List.class);
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


//        String inputResource = exchange.getIn().getBody(String.class);
        String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
        String updatedResource = updatedInputBundle(inputResource);
        // Set the updated resource back into the exchange body
        exchange.getIn().setBody(updatedResource);
    }

    public String updatedInputBundle(String inputResource) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(inputResource);

        if (rootNode != null || rootNode.isObject()) {
            // Extract the "entry" array from the bundle
            JsonNode entryArray = rootNode.path("entry");
            if (entryArray.isArray()) {
                // Update all references in the "entry" array
                for (JsonNode entry : entryArray) {
                    JsonNode resourceNode = entry.path("resource");
                    if (resourceNode.isObject()) {
                        updateReferences(resourceNode);
                    }
                }
            }
        }
        // Return the updated JSON as a string
        return objectMapper.writeValueAsString(rootNode);
    }

    private void updateReferences(JsonNode resourceNode) {
        Iterator<String> fieldNames = resourceNode.fieldNames();
        String regex = "^[^/]+/[A-Za-z0-9-]+$";
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode childNode = resourceNode.get(fieldName);

            if ("reference".equals(fieldName) && childNode.isTextual()) {
                String inputResourceId = childNode.asText();
                if (Pattern.matches(regex, inputResourceId)) {
                    {
                        Optional<String> dbInternalResourceId = resourceCompositionRepository.findById(inputResourceId)
                                .map(ResourceComposition::getInternalResourceId);

                        if (dbInternalResourceId.isPresent() && resourceNode instanceof ObjectNode) {
                            ((ObjectNode) resourceNode).put(fieldName, dbInternalResourceId.get());
                        }
                        break;
                    }
                }
            } else if (childNode.isObject()) {
                // Recursively update references in child objects
                updateReferences(childNode);
            } else if (childNode.isArray()) {
                // Recursively update references in array elements
                for (JsonNode arrayElement : childNode) {
                    updateReferences(arrayElement);
                }
            }
        }
    }
}
