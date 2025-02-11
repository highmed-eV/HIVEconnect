package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.ehrbase.fhirbridge.core.repository.ResourceCompositionRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
        List<String > inputResourceIds = exchange.getProperty(CamelConstants.INPUT_RESOURCE_IDS, List.class);
        Set<String> compositionIds = new HashSet<>();
        boolean hasNewResource = false;

        // fetch the compositionIds corresponding to the inputResourceIds from db
        for (String inputResourceId : inputResourceIds){
            Optional<String> optionalCompositionId = resourceCompositionRepository.findById(inputResourceId)
                    .map(ResourceComposition::getCompositionId);

            if (optionalCompositionId.isPresent()) {
                // Existing resource with a compositionId
                compositionIds.add(optionalCompositionId.get());
            } else {
                // New resource (compositionId not found in db)
                hasNewResource = true;
            }
        }

        // Check if all compositionIds are the same
        if (compositionIds.size() == 1 && !compositionIds.isEmpty() && !hasNewResource) {
            throw new IllegalArgumentException ("The input resource or input bundle contains resource(s) that already exist in the database. No new resources detected.");
        }
    }
}
