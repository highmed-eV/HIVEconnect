package org.ehrbase.fhirbridge.openfhir.openfhirclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
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
            String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
        
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity <String> entity = new HttpEntity<>(inputResource, headers);
            
            String openEhrJson = restTemplate.postForObject(openFhirUrl + "/openfhir/toopenehr", entity, String.class);
            logger.info("Received converted openEHR JSON.");
            
            return openEhrJson;
        } catch (Exception e) {
            throw new RuntimeException("Error in FHIR to openEHR conversion", e);
        }
    }

    public String ensureExistence(String templateId, OPERATIONALTEMPLATE operationaltemplate) {

        //check if template exists
        Boolean isExists = false;
        try {
            //TODO: Bug in openEHR:  openFHIR is throwing excption in case of not found scenario(404)
            // catch the excption and continue for now
            ResponseEntity<String> getEntity = restTemplate.getForEntity(openFhirUrl + "/opt", String.class);
            if (getEntity.getStatusCode().value() == HttpStatus.OK.value()  ){
                logger.debug("Template exists in openFHIR: {}", templateId);
                isExists = true;
                //TODO: Bug in openEHR: PUT is throwing exception if the id already exists.
                //Hence returning for now, without update.
                return getEntity.getBody();
            }
        } catch (Exception e) {
            logger.debug("Template does not exists in openFHIR: uploading template {}", templateId);
        }

        //upload the template
        try{

            XmlOptions opts = new XmlOptions();
            opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
 
            String response = null;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity <String> entity = new HttpEntity<>(operationaltemplate.xmlText(opts), headers);
            if (isExists) {
                //update
                restTemplate.put(openFhirUrl + "/opt/" + templateId, entity);
                response = "Updated template successfully: " + templateId ;
            } else {
                //create
                response = restTemplate.postForObject(openFhirUrl + "/opt", entity, String.class);
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error in upload template: {}", e);
        }
    }
}
