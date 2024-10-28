package org.ehrbase.fhirbridge.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OpenFHIRAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenFHIRAdapter.class);

    private final RestTemplate restTemplate;

    @Value("${openfhir.server.url}")
    private String openFhirUrl;

    public OpenFHIRAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String convertToOpenEHR(Exchange exchange) {
        try {
            logger.info("Calling openFHIR to convert FHIR JSON to openEHR format...");
            String inputResource = (String) exchange.getIn().getHeader("CamelFhirBridgeIncomingResource");
        
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity <String> entity = new HttpEntity<String>(inputResource, headers);
            
            String openEhrJson = restTemplate.postForObject(openFhirUrl + "/openfhir/toopenehr", entity, String.class);
            logger.info("Received converted openEHR JSON.");
            // logger.info(">>>> " + openEhrJson);
            
            return openEhrJson;
        } catch (Exception e) {
            logger.error("Error while converting FHIR to openEHR.", e);
            throw new RuntimeException("Error in FHIR to openEHR conversion", e);
        }
    }
}
