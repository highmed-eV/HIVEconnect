package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component(CompositionLookupProcessor.BEAN_ID)
public class CompositionLookupProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "compositionLookupProcessor";

    private final ResourceCompositionRepository resourceCompositionRepository;

    public CompositionLookupProcessor(ResourceCompositionRepository resourceCompositionRepository) {
        this.resourceCompositionRepository = resourceCompositionRepository;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        List<String> inputResourceIds = exchange.getProperty(CamelConstants.FHIR_REQUEST_RESOURCE_IDS, List.class);
        if (inputResourceIds == null || inputResourceIds.isEmpty()) {
            return;
        }

        Set<String> compositionIds = new HashSet<>();
        boolean isDuplicate = false;

        String operation = (String) exchange.getMessage().getHeader(CamelConstants.REQUEST_HTTP_METHOD);
        if ("POST".equals(operation)) {        
            // Fetch compositionIds corresponding to inputResourceIds from the db
            for (String inputResourceId : inputResourceIds) {
                // Fetch all compositionIds associated with the current inputResourceId
                List<String> compositionIdList = resourceCompositionRepository.findCompositionIdsByInputResourceId(inputResourceId);

                if (!compositionIdList.isEmpty()) {
                    // Add all compositionIds to the set
                    compositionIds.addAll(compositionIdList);
                }
            }

            // Now process each compositionId
            for (String compositionId : compositionIds) {
                // Fetch the list of input resources already in the composition
                List<String> existingResources = resourceCompositionRepository.findInputResourcesByCompositionId(compositionId);

                // Check if the current inputResourceIds are a subset of the existing resources in the composition
                if (existingResources.containsAll(inputResourceIds)) {
                    // If a composition already has the input resources or a subset, mark as duplicate
                    isDuplicate = true;
                    break; // No need to check further, a duplicate has been found
                }
            }

            // If a duplicate was found, throw an exception
            if (isDuplicate) {
                throw new IllegalArgumentException("The input resource or input bundle contains resource(s) that already exist in the database. No new resources detected.");
            }
        } else {
            // PUT
            for (String inputResourceId : inputResourceIds) {
                // Fetch all compositionIds associated with the current inputResourceId
                List<String> compositionIdList = resourceCompositionRepository.findCompositionIdsByInternalResourceId(inputResourceId);

                if (!compositionIdList.isEmpty()) {
                    // Add all compositionIds to the set
                    compositionIds.addAll(compositionIdList);
                }
            }
            // Now process each compositionId for PUT 
            for (String compositionId : compositionIds) {
                // Fetch the list of input resources already in the composition
                List<String> existingResources = resourceCompositionRepository.findInternalResourcesByCompositionId(compositionId);

                // Check if the current inputResourceIds are a subset of the existing resources in the composition
                if (existingResources.containsAll(inputResourceIds)) {
                    // If a composition already has the input resources or a subset, mark as duplicate
                    isDuplicate = true;
                    exchange.getMessage().setHeader(CamelConstants.OPENEHR_COMPOSITION_ID, compositionId);
                    break; // No need to check further, a duplicate has been found
                }
            }

            // If a duplicate was not found, throw an exception
            if (!isDuplicate) {
                throw new IllegalArgumentException("The input resource or input bundle contains resource(s) that does not exist. Cannot update resources.");
            }
        }

    }
}
