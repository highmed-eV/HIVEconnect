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
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.nedap.archie.rm.composition.Composition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.config.DebugProperties;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public static final String IDENTIFIER = "identifier";

    private final ResourceCompositionRepository resourceCompositionRepository;

    DebugProperties debugProperties;

    @Autowired
    public ProvideResourceResponseProcessor(ResourceCompositionRepository resourceCompositionRepository, DebugProperties debugProperties) {
        this.resourceCompositionRepository = resourceCompositionRepository;
        this.debugProperties = debugProperties;
    }

    @Override
    public void process(@NotNull Exchange exchange) throws Exception {
        // Logic to update the resource to composition

        Composition composition = exchange.getIn().getBody(Composition.class);
        String inputResource = exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE, String.class);
        String inputResourceType = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE_TYPE);
        // map to store the corresponding inputResourceId  and internalResourceId
        Map<String, String> resourceIdMap = new LinkedHashMap<>();

        if (!"Bundle".equals(inputResourceType)) {
            processSingleResource(exchange);
        }
        else 
        {
            JSONObject inputJsonObject = new JSONObject(inputResource);
            processBundle(exchange, inputJsonObject, resourceIdMap);
        }

        // Update database with resourceIdMap of inputResourceId  and internalResourceId
        // with compositionId
        updateResourceCompositions(resourceIdMap, composition);
    }

    private void processSingleResource(Exchange exchange) {
        //if not bundle take fhir response as MethodOutcome
        MethodOutcome outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, MethodOutcome.class);
       
        //Set response in exchange body
        exchange.getIn().setBody(outcome);
    }

    private void processBundle(Exchange exchange, JSONObject inputJsonObject, Map<String, String> resourceIdMap) throws IOException {
        //get inputresourceIds
        JSONArray entries = inputJsonObject.optJSONArray("entry");
        if (entries != null) {
            extractResourceIds(resourceIdMap, entries);
        }

        //if bundle take fhir server response as String
        String outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, String.class);
        JSONObject jsonObject = new JSONObject(outcome);

        //Get server response resource ids
        if (!jsonObject.isEmpty()) {
            JSONArray serverEntries = jsonObject.optJSONArray("entry");
            if (serverEntries != null) {
                processServerEntries(resourceIdMap, serverEntries);
            }
        }

        // merge and store the responses from FHIR_SERVER_OUTCOME, OPEN_FHIR_SERVER_OUTCOME and OPEN_EHR_SERVER_OUTCOME
        debugProperties.saveMergedServerResponses(exchange);

        //Set response in exchange body
        MethodOutcome methodOutcome = new MethodOutcome();
        String processedBundle = (String) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);

        FhirContext fhirContext = FhirContext.forR4();
        Bundle processedBundleResource = fhirContext.newJsonParser().parseResource(Bundle.class, processedBundle);

        // Set the MethodOutcome
        methodOutcome.setCreated(true);
        methodOutcome.setResource(processedBundleResource);
        exchange.getMessage().setBody(methodOutcome);
    }

    private static void extractResourceIds(Map<String, String> resourceIdMap, JSONArray entries) {
        for (int i = 0; i < entries.length(); i++) {
            JSONObject resource = entries.getJSONObject(i).optJSONObject("resource");
            if (resource != null) {
                String inputResourceId = null;
                if (resource.optString("resourceType").equals("Patient")) {
                    if (resource.has(IDENTIFIER) && resource.getJSONArray(IDENTIFIER).length() > 0) {
                        JSONObject firstIdentifier = resource.getJSONArray(IDENTIFIER).getJSONObject(0);
                        String system = firstIdentifier.getString("system");
                        String value = firstIdentifier.getString("value");
                        inputResourceId = system + "|" + value;
                    }
                } else {
                    inputResourceId = resource.optString("resourceType") + "/" + resource.optString("id");
                }
                // Placeholder for internalResourceId
                resourceIdMap.put(inputResourceId, null);
            }
        }
    }

    private void processServerEntries(Map<String, String> resourceIdMap, JSONArray serverEntries) {
        for (int i = 0; i < serverEntries.length(); i++) {
            JSONObject response = serverEntries.getJSONObject(i).optJSONObject("response");
            if (response != null) {
                String location = response.optString("location");
                if (!location.isEmpty()) {
                    // Extract resource ID (e.g., "Condition/20")
                    String internalResourceId = location.split("/_history")[0];
                    String correspondingInputResourceId = findInputResourceIdByIndex(resourceIdMap, i);
                    // Validate and update map
                    validateAndUpdateMap(resourceIdMap, correspondingInputResourceId, internalResourceId);
                }
            }
        }
    }

    private void updateResourceCompositions(Map<String, String> resourceIdMap, Composition composition) {
        for (Map.Entry<String, String> entry : resourceIdMap.entrySet()) {
            String inputResourceId = entry.getKey();
            String internalResourceId = entry.getValue();
            String compositionId = getCompositionId(composition);

            ResourceComposition resourceComposition = resourceCompositionRepository.findByInputResourceIdAndCompositionId(inputResourceId, compositionId)
                    .orElse(new ResourceComposition(inputResourceId, compositionId, internalResourceId, null));

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
        if (index >= 0 && index < keys.size()) {
            return keys.get(index);
        }
        return null;
    }

    private void validateAndUpdateMap(Map<String, String> resourceIdMap, String inputResourceId, String internalResourceId) {
        if (inputResourceId == null || inputResourceId.isEmpty()) {
            throw new IllegalArgumentException("Invalid inputResourceId: " + inputResourceId);
        }

        resourceIdMap.compute(inputResourceId, (key, existingInternalId) -> {
            if (existingInternalId != null && !existingInternalId.equals(internalResourceId)) {
                throw new IllegalStateException("Conflict: inputResourceId '" + inputResourceId +
                        "' is already mapped to a different internalResourceId: " + existingInternalId);
            }
            return internalResourceId;
        });
    }
}