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
public class OpenEHRAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenEHRAdapter.class);

    private final RestTemplate restTemplate;

    @Value("${openehr.server.url}")
    private String openEhrUrl;

    public OpenEHRAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String commitComposition(String ehrJson, Exchange exchange) {
        try {
            String ehrUid = (String) exchange.getIn().getHeader("EHRId");
        
            logger.info("Committing composition to openEHR server...");
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity <String> entity = new HttpEntity<String>(ehrJson, headers);
            String url = openEhrUrl + "/v1/ehr/" + ehrUid + "/composition";
            
            String response = restTemplate.postForObject(url, entity, String.class);
            logger.info("Composition successfully committed to openEHR server.");
            return response;
        } catch (Exception e) {
            logger.error("Error while committing composition to openEHR server.", e);
            throw new RuntimeException("Error in committing composition", e);
        }
    }
}
