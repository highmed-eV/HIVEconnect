package org.highmed.hiveconnect.fhir.camel;

import org.apache.camel.Exchange;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.highmed.hiveconnect.camel.component.ehr.composition.CompositionConstants;
import org.highmed.hiveconnect.camel.processor.FhirRequestProcessor;
import org.highmed.hiveconnect.core.repository.ResourceCompositionRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

        String operation = (String) exchange.getMessage().getHeader(CamelConstants.REQUEST_HTTP_METHOD);
        UUID ehrId = (UUID) exchange.getMessage().getHeader(CompositionConstants.EHR_ID);
        if ("POST".equals(operation)) {        
            // Fetch compositionIds corresponding to inputResourceIds from the db
            handlePost(ehrId, inputResourceIds);

        } else {
            // PUT
            handlePut(exchange, ehrId, inputResourceIds);
        }

    }

    private void handlePost(UUID ehrId, List<String> inputResourceIds) {
        Set<String> compositionIds = new HashSet<>();
        boolean isDuplicate = false;

        
        for (String inputResourceId : inputResourceIds) {
            // Fetch all compositionIds associated with the current inputResourceId
            List<String> compositionIdList = resourceCompositionRepository.findCompositionIdsByInputResourceIdAndEhrId(inputResourceId,ehrId);

            if (!compositionIdList.isEmpty()) {
                // Add all compositionIds to the set
                compositionIds.addAll(compositionIdList);
            }
        }

        // Now process each compositionId
        for (String compositionId : compositionIds) {
            // Fetch the list of input resources already in the composition
            List<String> existingResources = resourceCompositionRepository.findInputResourcesByCompositionIdAndEhrId(compositionId,ehrId);

            // Check if the current inputResourceIds are a subset of the existing resources in the composition
            if (new HashSet<>(existingResources).containsAll(inputResourceIds)) {
                // If a composition already has the input resources or a subset, mark as duplicate
                isDuplicate = true;
                break; // No need to check further, a duplicate has been found
            }
        }

        // If a duplicate was found, throw an exception
        if (isDuplicate) {
            throw new IllegalArgumentException("The input resource or input bundle contains resource(s) that already exist in the database. No new resources detected.");
        }

    }

    private void handlePut(Exchange exchange, UUID ehrId, List<String> inputResourceIds) {
        Set<String> compositionIds = new HashSet<>();
        boolean isDuplicate = false;

        
        for (String inputResourceId : inputResourceIds) {
            // Fetch all compositionIds associated with the current inputResourceId
            List<String> compositionIdList = resourceCompositionRepository.findCompositionIdsByInternalResourceIdAndEhrId(inputResourceId,ehrId);

            if (!compositionIdList.isEmpty()) {
                // Add all compositionIds to the set
                compositionIds.addAll(compositionIdList);
            }
        }
        // Now process each compositionId for PUT 
        for (String compositionId : compositionIds) {
            // Fetch the list of input resources already in the composition
            List<String> existingResources = resourceCompositionRepository.findInternalResourcesByCompositionIdAndEhrId(compositionId,ehrId);

            // Check if the current inputResourceIds are a subset of the existing resources in the composition
            if (new HashSet<>(existingResources).containsAll(inputResourceIds)) {
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
