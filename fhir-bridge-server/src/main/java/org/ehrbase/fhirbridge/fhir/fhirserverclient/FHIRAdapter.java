package org.ehrbase.fhirbridge.fhir.fhirserverclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ca.uhn.fhir.rest.api.server.IBundleProvider;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class FHIRAdapter {

    private static final Logger logger = LoggerFactory.getLogger(FHIRAdapter.class);

    private final RestTemplate restTemplate;

    @Value("${fhir.server.url}")
    private String fhirServerUrl;

    public FHIRAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String processFHIRRequest(String fhirRequestJson) {
        try {
            //Will not need this.Moved this to camel fhir
            // .to("fhir://create/resource?inBody=resourceAsString")
            logger.info("Forwarding FHIR request to FHIR server...");
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity <String> entity = new HttpEntity<String>(fhirRequestJson, headers);
            
            String response = restTemplate.postForObject(fhirServerUrl + "/fhir", entity, String.class);
            logger.info("Received response from FHIR server." + response);
            
            //TODO update exchange map for FHIR_RESOURCE_ID. 
            //Then store this with OPENEHR_COMPOSIRION)ID in db for further reference

            return response;
        } catch (Exception e) {
            logger.error("Error while processing FHIR request with FHIR server.");
            throw new RuntimeException("Error in FHIR processing", e);
        }
    }

    // public Set<ResourcePersistentId> searchForIds() {

    // }

    // public IBundleProvider search() {

    // }

    // public IBundleProvider create() {

    // }
}

