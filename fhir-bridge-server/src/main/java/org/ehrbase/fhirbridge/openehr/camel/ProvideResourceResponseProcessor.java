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
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        //TODO: Logic to update the resource to composition

        Composition composition = exchange.getIn().getBody(Composition.class);


        MethodOutcome outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, MethodOutcome.class);
        if (!Objects.isNull(outcome)) {
            // Use FHIR context to serialize OperationOutcome resource to JSON
//            FhirContext fhirContext = FhirContext.forR4();
//            String json = fhirContext.newJsonParser().encodeResourceToString((IBaseResource) outcome);
            JSONObject jsonObject = new JSONObject(outcome);
            List<String> resourceIds = new ArrayList<>();

            // Check if resourceType is "Bundle"
            if ("Bundle".equalsIgnoreCase(jsonObject.optString("resourceType"))) {
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
            } else {
//              String resourceId = outcome.getId().getIdPart();
                String resourceId = jsonObject.getString("resourceType") + "/" + jsonObject.getString("id");
                resourceIds.add(resourceId);
            }
            for(String resourceId : resourceIds) {
                ResourceComposition resourceComposition = resourceCompositionRepository.findById(resourceId)
                        .orElse(new ResourceComposition(resourceId));
                resourceComposition.setCompositionId(getCompositionId(composition));
                resourceCompositionRepository.save(resourceComposition);
                LOG.debug("Saved ResourceComposition: resourceId={}, compositionId={}", resourceComposition.getResourceId(), resourceComposition.getCompositionId());
            }
        }
        // Log the response and set it in the exchange
        LOG.info("ProvideResourceResponseProcessor");
        String response = (String) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME) +
//                (String) exchange.getMessage().getHeader(CamelConstants.FHIR_SERVER_OUTCOME) +
                "\n" +
                (String) exchange.getMessage().getHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME) +
                "\n" +
                (String) exchange.getMessage().getHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME);
        exchange.getMessage().setBody(response);
    }

    private String getCompositionId(Composition composition) {
        return composition.getUid().toString();
    }
}