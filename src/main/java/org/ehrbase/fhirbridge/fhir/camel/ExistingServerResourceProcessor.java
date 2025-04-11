package org.ehrbase.fhirbridge.fhir.camel;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component(ExistingServerResourceProcessor.BEAN_ID)
public class ExistingServerResourceProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "existingServerResourceProcessor";
    private final ObjectMapper objectMapper;

    public ExistingServerResourceProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
            // Retrieve existing resources or initialize list
            Object property = exchange.getProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, List.class);
            List<String> existingResources = objectMapper.convertValue(
                    property, new TypeReference<>() {}
            );
            // Add resource response to the existing resources list
            Resource resourceResponse = exchange.getIn().getBody(Resource.class);
            // Convert the resource to String using HAPI FHIR JSON parser
            FhirContext fhirContext = FhirContext.forR4();
            String resourceResponseStr = fhirContext.newJsonParser().encodeResourceToString(resourceResponse);
            existingResources.add(resourceResponseStr);
            exchange.setProperty(CamelConstants.FHIR_SERVER_EXISTING_RESOURCES, existingResources);
        }
    }
}
