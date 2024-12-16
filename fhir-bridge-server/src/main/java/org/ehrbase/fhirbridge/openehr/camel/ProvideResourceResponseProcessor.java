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
import ca.uhn.fhir.rest.api.server.RequestDetails;
import net.minidev.json.JSONValue;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// import jakarta.ws.rs.core.Response;
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
        Composition composition = exchange.getIn().getBody(Composition.class);

        //if not bundle take it as MethodOutcome
        List<String> resourceIds = new ArrayList<>();
        String inputResourceType = (String)exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE_TYPE);
        // boolean isBundle = "Bundle".equalsIgnoreCase(inputResourceType)?true:false;
        if(!"Bundle".equals(inputResourceType)) {
                MethodOutcome outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, MethodOutcome.class);
                FhirContext fhirContext = FhirContext.forR4(); 
                //TODO: serialize of Methodoutcome resulting in infinite loop
                //Hence doing only outcome.getResource()
                //move new ObjectMapper() to util
                IParser parser = fhirContext.newJsonParser();
                //Patient patient = parser.parseResource(Patient.class, outcome);
                String jsonString = parser.encodeResourceToString(outcome.getResource());
                // Convert JSON string to JsonNode using Jackson
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(jsonString);
                exchange.getIn().setBody(jsonNode);
                
                String resourceId = outcome.getId().getIdPart();
                String resourceIdStr = inputResourceType + "/" + resourceId;
                resourceIds.add(resourceIdStr);
                //TODO Check if this needs to be stored in the db?

        } else {
            String outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, String.class);
            JSONObject jsonObject = new JSONObject(outcome);
        
            if (!Objects.isNull(jsonObject)) {
            // Use FHIR context to serialize OperationOutcome resource to JSON
//            FhirContext fhirContext = FhirContext.forR4();
//            String json = fhirContext.newJsonParser().encodeResourceToString((IBaseResource) outcome);

                //Get resource ids
                JSONArray entries = jsonObject.optJSONArray("entry");
                if (entries != null) {
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject response = entries.getJSONObject(i).optJSONObject("response");
                        if (response != null) {
                            String location = response.optString("location");
                            if (!location.isEmpty()) {
                                // Extract resource ID (e.g., "Condition/20")
                                String resourceId = location.split("/_history")[0];
                                resourceIds.add(resourceId);
                            }
                        }
                    }
                }
                for(String resourceId : resourceIds) {
                    ResourceComposition resourceComposition = resourceCompositionRepository.findById(resourceId)
                            .orElse(new ResourceComposition(resourceId));
                    resourceComposition.setCompositionId(getCompositionId(composition));
                    resourceCompositionRepository.save(resourceComposition);
                    LOG.debug("Saved ResourceComposition: resourceId={}, compositionId={}", resourceComposition.getResourceId(), resourceComposition.getCompositionId());
                }
            }
        
            // Jackson ObjectMapper
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
        // Log the response and set it in the exchange
        LOG.info("ProvideResourceResponseProcessor");

    }

    private String getCompositionId(Composition composition) {
        return composition.getUid().toString();
    }
}