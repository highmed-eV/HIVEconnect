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

import com.apicatalog.jsonld.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final String RESOURCE_TYPE = "resourceType";
    public static final String RESOURCE = "resource";

    private final ResourceCompositionRepository resourceCompositionRepository;

    public ExistingResourceReferenceProcessor(ResourceCompositionRepository resourceCompositionRepository) {
        this.resourceCompositionRepository = resourceCompositionRepository;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String systemId = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID);
        ObjectMapper objectMapper = new ObjectMapper();

        // Fetch required properties from the exchange
        List<String> existingResources = exchange.getProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, List.class);

        // replace the ids in the existing fhir server resources
        // with the inputResourceIds corresponding to that in the db
        // according to the mapping table : FB_RESOURCE_COMPOSITION
        if (existingResources != null && !existingResources.isEmpty()) {
            mapToInputResourceId(existingResources, objectMapper, systemId);
        }

        // Update the Exchange property with the modified list
        exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, existingResources);

        // Parse the input JSON
        String inputResourceBundle = (String) exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING);
        JsonNode rootNode = objectMapper.readTree(inputResourceBundle);

        if (!isValidBundle(rootNode)) {
            return; // No valid entries or bundle to process
        }
        // Get the "entry" array
        ArrayNode entryArray = (ArrayNode) rootNode.get("entry");
        // Collect existing IDs from the input bundle
        Set<String> processedIds = getProcessedIds(entryArray);

        // Add existing resources that are not already in the bundle
        addExistingResources(existingResources, objectMapper, processedIds, entryArray);

        // Update the bundle in the exchange
        String updatedBundleJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        FhirContext fhirContext = FhirContext.forR4();
        JsonParser jsonParser = (JsonParser) fhirContext.newJsonParser();
        Bundle bundleResource =  jsonParser.parseResource(Bundle.class, updatedBundleJson);
        exchange.getIn().setBody(bundleResource);
    }

    private List<String> mapToInputResourceId(List<String> existingResources, ObjectMapper objectMapper, String systemId) throws JsonProcessingException {
        for (int i = 0; i < existingResources.size(); i++) {
            String resourceJson = existingResources.get(i);
            JsonNode resourceNode = objectMapper.readTree(resourceJson);
            // Extract the ID from the resource
            if (resourceNode.has(RESOURCE_TYPE) && resourceNode.has("id")) {
                String resourceId = resourceNode.get(RESOURCE_TYPE).asText() + "/" + resourceNode.get("id").asText();

                // Fetch replacement IDs from the database
                String dbInputResourceId = resourceCompositionRepository.findInternalResourceIdByInputResourceIdAndSystemId(resourceId, systemId);

                if(StringUtils.isNotBlank(dbInputResourceId)){
                    Matcher matcher = Pattern.compile("([^/]+)/([^/]+)").matcher(dbInputResourceId);

                    if (matcher.matches()) {
                        ((ObjectNode) resourceNode).put("id", matcher.group(2));
                        // Replace the original JSON string in the list with the updated one
                        existingResources.set(i, objectMapper.writeValueAsString(resourceNode));
                    }
                }
            }
        }
        return existingResources;
    }

    private boolean isValidBundle(JsonNode rootNode) {
        return rootNode.has("entry") && rootNode.has(RESOURCE_TYPE) && "Bundle".equals(rootNode.get(RESOURCE_TYPE).asText());
    }

    private @NotNull Set<String> getProcessedIds(ArrayNode entryArray) {
        Set<String> processedIds = new HashSet<>();

        for (JsonNode entryNode : entryArray) {
            JsonNode resource = entryNode.path(RESOURCE);
            if (resource.has(RESOURCE_TYPE) && resource.has("id")) {
                String inputResourceId = resource.get(RESOURCE_TYPE).asText() +
                        "/" + resource.get("id").asText();
                processedIds.add(inputResourceId);
            }
        }
        return processedIds;
    }

    private void addExistingResources(List<String> existingResources, ObjectMapper objectMapper, Set<String> processedIds, ArrayNode entryArray) throws JsonProcessingException {
        if (existingResources != null && !existingResources.isEmpty()) {
            for (String existingResourceJson : existingResources) {
                JsonNode newResourceNode = objectMapper.readTree(existingResourceJson);
                if (newResourceNode.has(RESOURCE_TYPE) && newResourceNode.has("id")) {
                    String newResourceId = newResourceNode.get(RESOURCE_TYPE).asText() +
                            "/" + newResourceNode.get("id").asText();
                    if (!processedIds.contains(newResourceId)) {
                        // Create a new entry for the existing resource
                        ObjectNode newEntryNode = objectMapper.createObjectNode();
                        newEntryNode.put("fullUrl", newResourceId);
                        newEntryNode.set(RESOURCE, newResourceNode);
                        entryArray.add(newEntryNode);
                    }
                }
            }
        }
    }


}
