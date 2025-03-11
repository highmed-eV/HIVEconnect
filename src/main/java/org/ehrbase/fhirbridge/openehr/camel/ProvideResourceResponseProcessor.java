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
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final FhirContext fhirContext;
    private final DebugProperties debugProperties;

    @Autowired
    public ProvideResourceResponseProcessor(ResourceCompositionRepository resourceCompositionRepository, DebugProperties debugProperties) {
        this.resourceCompositionRepository = resourceCompositionRepository;
        this.debugProperties = debugProperties;
        this.fhirContext = FhirContext.forR4();
    }

    @Override
    public void process(@NotNull Exchange exchange) throws Exception {
        Composition composition = (Composition) exchange.getIn().getHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME_COMPOSITION);
        Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
        String inputResourceType = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE);
        Map<String, String> resourceIdMap = new LinkedHashMap<>();

        if (!"Bundle".equals(inputResourceType)) {
            if ("Patient".equals(inputResourceType)) {
                processPatientResource(exchange);
            } else {
                processSingleResource(exchange, inputResource, resourceIdMap);
            }
        } else {
            processBundle(exchange, (Bundle) inputResource, resourceIdMap);
        }

        updateResourceCompositions(exchange, inputResourceType, resourceIdMap, composition);
    }

    private void processPatientResource(Exchange exchange) {
        MethodOutcome outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, MethodOutcome.class);
        exchange.getIn().setBody(outcome);
    }

    private void processSingleResource(Exchange exchange, Resource resource, Map<String, String> resourceIdMap) {
        MethodOutcome outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME, MethodOutcome.class);
        
        String inputResourceId = resource.getId();
        String internalId = outcome.getId().getValue();
        String internalResourceId = internalId.split("/_history")[0];
                
        resourceIdMap.put(inputResourceId, internalResourceId);

        exchange.getIn().setBody(outcome);
    }

    private void processBundle(Exchange exchange, Bundle inputBundle, Map<String, String> resourceIdMap) {
        try {
            // Extract input resource IDs
            for (Bundle.BundleEntryComponent entry : inputBundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource != null) {
                    String inputResourceId = extractResourceId(resource);
                    if (inputResourceId != null) {
                        resourceIdMap.put(inputResourceId, null);
                    }
                }
            }

            // Process server response
            Object serverResponse = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
            Bundle serverBundle;
            
            // Convert response to JSON string for consistent handling
            String jsonString;
            if (serverResponse instanceof Resource) {
                jsonString = fhirContext.newJsonParser().encodeResourceToString((Resource) serverResponse);
                // Update the exchange property to maintain compatibility with DebugProperties
                // exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, jsonString);
                serverBundle = (Bundle) serverResponse;
            } else if (serverResponse instanceof MethodOutcome) {
                IBaseResource baseResource = ((MethodOutcome) serverResponse).getResource();
                if (baseResource == null) {
                    throw new IllegalArgumentException("MethodOutcome does not contain a resource");
                }
                if (!(baseResource instanceof Bundle)) {
                    throw new IllegalArgumentException("MethodOutcome contains invalid resource type: " + 
                        baseResource.getClass().getName() + ". Expected Bundle.");
                }
                serverBundle = (Bundle) baseResource;
            } else if (serverResponse instanceof String) {
                jsonString = (String) serverResponse;
                serverBundle = fhirContext.newJsonParser().parseResource(Bundle.class, jsonString);
            } else {
                throw new IllegalArgumentException("Unexpected server response type: " + 
                    (serverResponse != null ? serverResponse.getClass().getName() : "null"));
            }

            processServerResponse(resourceIdMap, serverBundle);

            // Debug properties
            debugProperties.saveMergedServerResponses(exchange);

            // Set response in exchange body
            MethodOutcome methodOutcome = new MethodOutcome();
            methodOutcome.setCreated(true);
            methodOutcome.setResource(serverBundle);
            exchange.getMessage().setBody(methodOutcome);
        } catch (Exception e) {
            throw new RuntimeException("Error processing bundle: " + e.getMessage(), e);
        }
    }

    private String extractResourceId(Resource resource) {
        if (resource instanceof Patient) {
            Patient patient = (Patient) resource;
            Optional<Identifier> identifier = patient.getIdentifier().stream().findFirst();
            return identifier.map(id -> id.getSystem() + "|" + id.getValue()).orElse(null);
        } else {
            String resourceType = resource.getResourceType().name();
            String id = resource.getId();
            if (!id.startsWith(resourceType + "/")) {
                return resourceType + "/" + id;
            }
            return id;
        }
    }

    private void processServerResponse(Map<String, String> resourceIdMap, Bundle responseBundle) {
        List<String> inputResourceIds = resourceIdMap.keySet().stream().toList();
        
        for (int i = 0; i < responseBundle.getEntry().size(); i++) {
            Bundle.BundleEntryComponent entry = responseBundle.getEntry().get(i);
            if (entry.hasResponse() && entry.getResponse().hasLocation()) {
                String location = entry.getResponse().getLocation();
                String internalResourceId = location.split("/_history")[0];
                
                if (i < inputResourceIds.size()) {
                    String inputResourceId = inputResourceIds.get(i);
                    validateAndUpdateMap(resourceIdMap, inputResourceId, internalResourceId);
                }
            }
        }
    }

    private void updateResourceCompositions(Exchange exchange, String inputResourceType, Map<String, String> resourceIdMap, Composition composition) {
        for (Map.Entry<String, String> entry : resourceIdMap.entrySet()) {
            String inputResourceId = entry.getKey();
            String internalResourceId = entry.getValue();
            String compositionId = getCompositionId(composition);
            ResourceComposition resourceComposition;

            String method =  (String) exchange.getIn().getHeader(CamelConstants.REQUEST_HTTP_METHOD);
            if ("POST".equals(method)) {
                resourceComposition = resourceCompositionRepository.findByInputResourceId(inputResourceId)
                        .orElse(new ResourceComposition(inputResourceId, compositionId, internalResourceId, null));
            } else {
                compositionId = (String) exchange.getMessage().getHeader(CamelConstants.OPENEHR_COMPOSITION_ID);
                resourceComposition = resourceCompositionRepository.findByInternalResourceIdAndCompositionId(inputResourceId, compositionId)
                        .orElse(new ResourceComposition(inputResourceId, compositionId, internalResourceId, null));
            }

            resourceComposition.setCompositionId(getCompositionId(composition));
            resourceComposition.setInternalResourceId(internalResourceId);
            resourceCompositionRepository.save(resourceComposition);

            LOG.debug("Saved ResourceComposition: inputResourceId={}, internalResourceId={}, compositionId={}",
                    resourceComposition.getInputResourceId(), resourceComposition.getInternalResourceId(), resourceComposition.getCompositionId());
        }

        LOG.info("ProvideResourceResponseProcessor");
    }

    private String getCompositionId(Composition composition) {
        return composition.getUid().toString();
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