package org.ehrbase.fhirbridge.openehr.openehrclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.nedap.archie.rm.composition.Composition;

import java.util.Arrays;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
// import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.TemporalAccessorDeSerializer;
// import org.ehrbase.openehr.sdk.generator.commons.aql.field.AqlField;
// import org.ehrbase.openehr.sdk.generator.commons.aql.field.ListSelectAqlField;
// import org.ehrbase.openehr.sdk.generator.commons.aql.parameter.ParameterValue;
// import org.ehrbase.openehr.sdk.generator.commons.aql.query.Query;
// import org.ehrbase.openehr.sdk.generator.commons.aql.record.RecordImp;
// import org.ehrbase.openehr.sdk.serialisation.mapper.RmObjectJsonDeSerializer;
// import org.ehrbase.openehr.sdk.util.exception.ClientException;
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
            UUID ehrUid = (UUID) exchange.getIn().getHeader(CompositionConstants.EHR_ID);
        
            logger.info("Committing composition to openEHR server...");
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity <String> entity = new HttpEntity<String>(ehrJson, headers);
            String url = openEhrUrl + "/v1/ehr/" + ehrUid + "/composition";
            
            String response = restTemplate.postForObject(url, entity, String.class);
            //Extract compoistionId
            logger.info("Composition successfully committed to openEHR server.");
            exchange.getMessage().setHeader(CompositionConstants.VERSION_UID, response);
            exchange.getMessage().setHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME, response);
            return response;
        } catch (Exception e) {
            logger.error("Error while committing composition to openEHR server.", e);
            throw new ClientException("Error in committing composition", e);
        }
    }


}
