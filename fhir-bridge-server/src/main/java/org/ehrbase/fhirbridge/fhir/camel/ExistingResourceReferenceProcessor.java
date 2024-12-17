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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link org.apache.camel.Processor Processor} that retrieves the internalResourceId corresponding to the inputResourceId involved in the current
 * exchange. If the resourceId is present in the db return the corresponding internalResourceIds.
 *
 * @since 1.2.0
 */
@Component(ExistingResourceReferenceProcessor.BEAN_ID)
@SuppressWarnings("java:S6212")
public class ExistingResourceReferenceProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "existingResourceReferenceProcessor";

    private static final Logger LOG = LoggerFactory.getLogger(ExistingResourceReferenceProcessor.class);

    private final ResourceCompositionRepository resourceCompositionRepository;

    public ExistingResourceReferenceProcessor(ResourceCompositionRepository resourceCompositionRepository) {
        this.resourceCompositionRepository = resourceCompositionRepository;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        // Fetch required properties from the exchange
        List<String> inputResourceIds = exchange.getProperty(CamelConstants.INPUT_RESOURCE_IDS, List.class);
        List<String> existingResources = exchange.getProperty(CamelConstants.SERVER_EXISTING_RESOURCES, List.class);

        // replace the ids in the existing fhir server resources
        // with the inputResourceIds corresponding to that in the db
        // the mapping table : FB_RESOURCE_COMPOSITION
        if (existingResources != null && !existingResources.isEmpty()) {
            existingResources = mapToInputResourceId(existingResources, objectMapper);
        }

        // Update the Exchange property with the modified list
        exchange.setProperty(CamelConstants.SERVER_EXISTING_RESOURCES, existingResources);

        // Parse the input JSON
        String bundleJson = exchange.getIn().getBody(String.class);
        JsonNode rootNode = objectMapper.readTree(bundleJson);

        if (!rootNode.has("entry")) {
            return; // No entries to process
        }
        if (rootNode.has("resourceType") && "Bundle".equals(rootNode.get("resourceType").asText())) {
            ArrayNode entryArray = (ArrayNode) rootNode.get("entry");
            for (JsonNode entryNode : entryArray) {
                String inputResourceId = entryNode.get("resource").get("resourceType").asText() + "/" + entryNode.get("resource").get("id").asText();

                // Check if the resource ID is in inputResourceIds
                if (inputResourceIds != null && inputResourceIds.contains(inputResourceId)) {
                    continue; // ID found, move to the next
                }
                // add the existing resource(s) in the input bundle if ID is not found
                // i.e., reference resources are added.
                if (existingResources != null && !existingResources.isEmpty()) {
                    for (String existingResourceJson : existingResources) {
                        // Parse existing resource JSON
                        JsonNode newResourceNode = objectMapper.readTree(existingResourceJson);
//
                        // Create a new entry for the existing resource
                        ObjectNode newEntryNode = objectMapper.createObjectNode();
                        newEntryNode.put("fullUrl", newResourceNode.get("resourceType").asText() + "/" + newResourceNode.get("id").asText());
                        newEntryNode.set("resource", newResourceNode);
//                        newEntryNode.set("request", createPostRequestNode(newResourceNode.get("resourceType").asText(), objectMapper));

                        // Add the new entry to the Bundle
                        entryArray.add(newEntryNode);
                    }
                }
            }

        }
        // Update the bundle in the exchange
        String updatedBundleJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        exchange.getIn().setBody(updatedBundleJson);

    }

    private List<String> mapToInputResourceId(List<String> existingResources, ObjectMapper objectMapper) throws JsonProcessingException {
        for (int i = 0; i < existingResources.size(); i++) {
            String resourceJson = existingResources.get(i);

            // Parse the JSON
            JsonNode resourceNode = objectMapper.readTree(resourceJson);

            // Extract the ID from the resource
            if (resourceNode.has("resourceType") && resourceNode.has("id")) {
                String resourceId = resourceNode.get("resourceType").asText() + "/" + resourceNode.get("id").asText();

                // Fetch replacement IDs from the database
                String dbInputResourceId = resourceCompositionRepository.getInputResourceIds(resourceId);

                String newResourceId = dbInputResourceId; // Assuming one-to-one mapping
                ((ObjectNode) resourceNode).put("id", newResourceId);

                // Replace the original JSON string in the list with the updated one
                existingResources.set(i, objectMapper.writeValueAsString(resourceNode));

            }
        }
        return existingResources;
    }

    private ObjectNode createPostRequestNode(String resourceType, ObjectMapper objectMapper) {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("method", "POST");
        requestNode.put("url", resourceType);
        return requestNode;
    }
}
