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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

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
@Component(ReferencedResourceLookupProcessor.BEAN_ID)
public class ReferencedResourceLookupProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "resourceLookupProcessor";

    private final ResourceCompositionRepository resourceCompositionRepository;

    public ReferencedResourceLookupProcessor(ResourceCompositionRepository resourceCompositionRepository) {
        this.resourceCompositionRepository = resourceCompositionRepository;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String systemId = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID);
        List<String > inputResourceIds = exchange.getProperty(CamelConstants.FHIR_REFERENCE_REQUEST_RESOURCE_IDS, List.class);
        if (inputResourceIds == null || inputResourceIds.isEmpty()) {
            return;
        }

        // fetch the internalResourceIds corresponding to the inputResourceIds from db
        List<String> internalResourceIds = resourceCompositionRepository.findInternalResourceIdsByInputResourceIdsAndSystemId(inputResourceIds, systemId);

        if (!internalResourceIds.isEmpty()) {
            exchange.setProperty(CamelConstants.FHIR_REFERENCE_INTERNAL_RESOURCE_IDS, internalResourceIds);
        }

        //Input taken as String
        String inputResource = (String) exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING);
        if (inputResource != null) {
            // update the input resource bundle reference with internalResourceIds
            updateInputResource(exchange, inputResource);
            // Set the updated resource back into the exchange body

        }
    }

    private String updateInputResource(Exchange exchange, String inputResource) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(inputResource);
        String systemId = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID);

        String updatedResource = inputResource;
        if (rootNode != null && rootNode.isObject()) {
            // if resourceType is Bundle
            if (rootNode.has("resourceType") && "Bundle".equals(rootNode.get("resourceType").asText())) {
                updateBundleReferences(rootNode, systemId);
                updatedResource = objectMapper.writeValueAsString(rootNode);
                FhirContext fhirContext = FhirContext.forR4();
                JsonParser jsonParser = (JsonParser) fhirContext.newJsonParser();
                Bundle updatedBundleResource =  jsonParser.parseResource(Bundle.class, updatedResource);   
                exchange.getIn().setBody(updatedBundleResource);
                exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING, updatedResource);

            } else {
                // for individual resource
                updateReferences(rootNode, systemId);
                //TODO: Set the updated resource back into the exchange body
            }
        }
        // Return the updated JSON as a string
        return updatedResource;
    }

    private void updateBundleReferences(JsonNode rootNode, String systemId) {
        // Extract the "entry" array from the bundle
        JsonNode entryArray = rootNode.path("entry");
        if (entryArray.isArray()) {
            // Update all references in the "entry" array
            for (JsonNode entry : entryArray) {
                JsonNode resourceNode = entry.path("resource");
                if (resourceNode.isObject()) {
                    updateReferences(resourceNode, systemId);
                }
            }
        }
    }

    private void updateReferences(JsonNode resourceNode, String systemId) {
        Iterator<String> fieldNames = resourceNode.fieldNames();
        String regex = "^[^/]+/[A-Za-z0-9-]+$";
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode childNode = resourceNode.get(fieldName);

            if ("reference".equals(fieldName) && childNode.isTextual()) {
                if (processReferenceField(resourceNode, childNode, regex, fieldName, systemId)) break;
            } else if (childNode.isObject()) {
                // Recursively update references in child objects
                updateReferences(childNode, systemId);
            } else if (childNode.isArray()) {
                // Recursively update references in array elements
                for (JsonNode arrayElement : childNode) {
                    updateReferences(arrayElement, systemId);
                }
            }
        }
    }

    private boolean processReferenceField(JsonNode resourceNode, JsonNode childNode, String regex, String fieldName, String systemId) {
        String inputResourceId = childNode.asText();
        if (Pattern.matches(regex, inputResourceId)) {
            Optional<String> dbInternalResourceId = resourceCompositionRepository.findByInputResourceIdAndSystemId(inputResourceId, systemId)
                    .map(ResourceComposition::getInternalResourceId);

            if (dbInternalResourceId.isPresent() && resourceNode instanceof ObjectNode objectNode) {
                objectNode.put(fieldName, dbInternalResourceId.get());
            }
            return true;
        }
        return false;
    }
}
