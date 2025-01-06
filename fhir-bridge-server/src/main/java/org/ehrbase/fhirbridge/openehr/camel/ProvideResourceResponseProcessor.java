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

package org.ehrbase.fhirbridge.openehr.camel;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nedap.archie.rm.composition.Composition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * {@link Processor} that stores the link between the FHIR resource and the openEHR composition.
 *
 * @since 1.0.0
 */
@Component(ProvideResourceResponseProcessor.BEAN_ID)
@SuppressWarnings("java:S6212")
public class ProvideResourceResponseProcessor implements Processor {

    public static final String BEAN_ID = "provideResourceResponseProcessor";

    private static final Logger LOG = LoggerFactory.getLogger(ProvideResourceResponseProcessor.class);

    private final ResourceCompositionRepository resourceCompositionRepository;

    public ProvideResourceResponseProcessor(ResourceCompositionRepository resourceCompositionRepository) {
        this.resourceCompositionRepository = resourceCompositionRepository;
    }

    @Override
    public void process(@NotNull Exchange exchange) throws Exception {
        // Logic to update the resource to composition

        Composition composition = exchange.getIn().getBody(Composition.class);
        String inputResource = exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE, String.class);
        String inputResourceType = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE_TYPE);
        // map to store the corresponding inputResourceId  and internalResourceId
        Map<String, String> resourceIdMap = new LinkedHashMap<>();

        JSONObject inputJsonObject = new JSONObject(inputResource);
        if (!"Bundle".equals(inputResourceType)) {
            //if not bundle take fhir response as MethodOutcome
            MethodOutcome outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, MethodOutcome.class);
            //TODO: serialize of Methodoutcome resulting in infinite loop
            //Hence not using JSONObject. And serializing only outcome.getResource()
            // JSONObject jsonObject = new JSONObject(outcome);
            FhirContext fhirContext = FhirContext.forR4();
            IParser parser = fhirContext.newJsonParser();
            String responseJsonString = parser.encodeResourceToString(outcome.getResource());
            ObjectMapper objectMapper = new ObjectMapper();
            //TODO: check take it as JsonObject
            JsonNode responseJsonNode = objectMapper.readTree(responseJsonString);

            //Set response in exchange body
            exchange.getIn().setBody(responseJsonNode);

        } else {
            //get inputresourceIds
            JSONArray entries = inputJsonObject.optJSONArray("entry");
            if (entries != null) {
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject resource = entries.getJSONObject(i).optJSONObject("resource");
                    if (resource != null) {
                        String inputResourceId = null;
                        if (resource.optString("resourceType").equals("Patient")) {
                            if (resource.has("identifier") && resource.getJSONArray("identifier").length() > 0) {
                                JSONObject firstIdentifier = resource.getJSONArray("identifier").getJSONObject(0);
                                String system = firstIdentifier.getString("system");
                                String value = firstIdentifier.getString("value");
                                inputResourceId = system + "|" + value;
                            }
                        } else {
                            inputResourceId = resource.optString("resourceType") + "/" + resource.optString("id");
                        }
                                 
                        // inputResourceIds.add(inputResourceId);
                        // Placeholder for internalResourceId
                        resourceIdMap.put(inputResourceId, null);
                    }
                }
            }

            //if bundle take fhir server response as String
            String outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, String.class);
            JSONObject jsonObject = new JSONObject(outcome);

            //Get server response resource ids
            if (!Objects.isNull(jsonObject)) {
                // Use FHIR context to serialize OperationOutcome resource to JSON
                //FhirContext fhirContext = FhirContext.forR4();
                //String json = fhirContext.newJsonParser().encodeResourceToString((IBaseResource) outcome);
                JSONArray serverEntries = jsonObject.optJSONArray("entry");
                if (serverEntries != null) {
                    for (int i = 0; i < serverEntries.length(); i++) {
                        JSONObject response = serverEntries.getJSONObject(i).optJSONObject("response");
                        if (response != null) {
                            String location = response.optString("location");
                            if (!location.isEmpty()) {
                                // Extract resource ID (e.g., "Condition/20")
                                String internalResourceId = location.split("/_history")[0];
                                // internalResourceIds.add(internalResourceId);
                                String correspondingInputResourceId = findInputResourceIdByIndex(resourceIdMap, i);
                                // Validate and update map
                                validateAndUpdateMap(resourceIdMap, correspondingInputResourceId, internalResourceId);
                            }
                        }
                    }
                }
            }

            //Set response in exchange body
            //move new ObjectMapper() to util
            ObjectMapper objectMapper = new ObjectMapper();
            // Parse JSON strings into JsonNode objects
            JsonNode node1 = objectMapper.readTree((String) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME));
            JsonNode node2 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME));
            JsonNode node3 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME));

            // Merge JsonNode objects into a single ObjectNode
            ObjectNode mergedJson = objectMapper.createObjectNode();
            mergedJson.setAll((ObjectNode) node1);
            mergedJson.setAll((ObjectNode) node2);
            mergedJson.setAll((ObjectNode) node3);

            exchange.getIn().setBody(mergedJson);

        }

        // Update database with resourceIdMap of inputResourceId  and internalResourceId
        // with compositionId
        for (Map.Entry<String, String> entry : resourceIdMap.entrySet()) {
            String inputResourceId = entry.getKey();
            String internalResourceId = entry.getValue();

            ResourceComposition resourceComposition = resourceCompositionRepository.findById(inputResourceId)
                    .orElse(new ResourceComposition(inputResourceId));

            resourceComposition.setCompositionId(getCompositionId(composition));
            resourceComposition.setInternalResourceId(internalResourceId);

            resourceCompositionRepository.save(resourceComposition);

            LOG.debug("Saved ResourceComposition: inputResourceId={}, internalResourceId={}, compositionId={}",
                    resourceComposition.getInputResourceId(), resourceComposition.getInternalResourceId(), resourceComposition.getCompositionId());
        }

        // Log the response and set it in the exchange
        LOG.info("ProvideResourceResponseProcessor");

    }

    private String getCompositionId(Composition composition) {
        return composition.getUid().toString();
    }

    private String  findInputResourceIdByIndex(Map<String, String> resourceIdMap, int index) {
        List<String> keys = new ArrayList<>(resourceIdMap.keySet());
        if (index < keys.size()) {
            return keys.get(index);
        }
        return null;
    }

    private void validateAndUpdateMap(Map<String, String> resourceIdMap, String inputResourceId, String internalResourceId) {
        if (inputResourceId == null || inputResourceId.isEmpty()) {
            throw new IllegalArgumentException("Invalid inputResourceId: " + inputResourceId);
        }

        if (resourceIdMap.containsKey(inputResourceId)) {
            String existingInternalId = resourceIdMap.get(inputResourceId);

            if (existingInternalId != null && !existingInternalId.equals(internalResourceId)) {
                throw new IllegalStateException("Conflict: inputResourceId '" + inputResourceId +
                        "' is already mapped to a different internalResourceId: " + existingInternalId);
            } else if (existingInternalId == null) {
                resourceIdMap.put(inputResourceId, internalResourceId);
            }
        } else {
            resourceIdMap.put(inputResourceId, internalResourceId);
        }
    }
}