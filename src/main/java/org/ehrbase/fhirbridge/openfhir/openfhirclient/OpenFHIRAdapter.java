package org.ehrbase.fhirbridge.openfhir.openfhirclient;

import org.ehrbase.fhirbridge.exception.ConversionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Optional;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.openehr.DefaultTemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OpenFHIRAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenFHIRAdapter.class);

    private final RestTemplate restTemplate;

    @Value("${openfhir.server.url}")
    private String openFhirUrl;

    private final DefaultTemplateProvider templateProvider;

    public OpenFHIRAdapter(RestTemplate restTemplate, DefaultTemplateProvider templateProvider) {
        this.restTemplate = restTemplate;
        this.templateProvider = templateProvider;
    }

    public String convertToOpenEHR(Exchange exchange) {
        try {
            logger.info("Calling openFHIR to convert FHIR JSON to openEHR format...");
            String inputResource = (String) exchange.getIn().getHeader(CamelConstants.TEMP_REQUEST_RESOURCE_STRING);
        
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity <String> entity = new HttpEntity<>(inputResource, headers);
            
            String openEhrJson = restTemplate.postForObject(openFhirUrl + "/openfhir/toopenehr", entity, String.class);
            logger.info("Received converted openEHR JSON.");
            
            return openEhrJson;
        } catch (Exception e) {
            throw new ConversionException("Error in FHIR to openEHR conversion", e);
        }
    }

    public boolean checkProfileSupported(Exchange exchange) {
        List<String> outputProfiles;
        List<String> inputProfiles = (List<String>) exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PROFILE);
        try {
            logger.info("Calling openFHIR to get profiles...");
            
            // Fetch the JSON response and convert it directly to List<String>
            ResponseEntity<List<String>> response = restTemplate.exchange(
                                                    openFhirUrl + "/fc/profiles",
                                                    HttpMethod.GET,
                                                    null,
                                                    new ParameterizedTypeReference<>() {}
                                                );
            // Extract the list of profiles from the response
            outputProfiles = response.getBody();
            logger.info("Received supported profiles from openEHR JSON.");

        } catch (Exception e) {
            throw new ConversionException("Error in getting openEHR supported profiles", e);
        }
        
        // Check if any inputProfiles are in outputProfiles
        boolean isProfilePresent = inputProfiles.stream()
                .anyMatch(outputProfiles::contains);
        if (!isProfilePresent) {
            throw new IllegalArgumentException("No matching profiles found in openFHIR");
        }
        return isProfilePresent;
    }

    public Optional<OPERATIONALTEMPLATE> findTemplate(String templateId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> getEntity = restTemplate.exchange(
                openFhirUrl + "/opt/" + templateId,
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
            );

            if (getEntity.getStatusCode().value() == HttpStatus.OK.value()) {
                try {
                    String responseBody = getEntity.getBody();
                    if (responseBody != null && !responseBody.trim().isEmpty()) {
                        XmlOptions opts = new XmlOptions();
                        opts.setLoadStripWhitespace();
                        opts.setLoadReplaceDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
                        
                        OPERATIONALTEMPLATE template = OPERATIONALTEMPLATE.Factory.parse(responseBody, opts);
                        return Optional.of(template);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing template response for template {}: {}", templateId, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Template {} does not exist in openFHIR", templateId);
        }
        return Optional.empty();
    }

    public String ensureExistence(String templateId) {
        //check if template exists in cache
        Optional<OPERATIONALTEMPLATE> operationaltemplate = templateProvider.find(templateId);
        if (operationaltemplate.isEmpty()) {
            logger.info("Template does not exist in Cache: {}", templateId);
            return "Template does not exist in Cache";
        }

        //check if template exists in openFHIR
        if (findTemplate(templateId).isPresent()) {
            logger.info("Template already exists in openFHIR: {}", templateId);
            return "Template already exists in openFHIR";
        }

        //upload the template
        try{
            XmlOptions opts = new XmlOptions();
            opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
 
            String response;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
            HttpEntity <String> entity = new HttpEntity<>(operationaltemplate.get().xmlText(opts), headers);
            //create
            response = restTemplate.postForObject(openFhirUrl + "/opt", entity, String.class);
            if (response.contains("already exists")) {
                logger.info("Template already exists in openFHIR after post: {}", templateId);
            } else {
                logger.info("Uploaded template to openFHIR: {}", templateId);
            }
            return response;
        } catch (Exception e) {
            if (e.getMessage().contains("already exists")) {
                logger.info("Template already exists in openFHIR: {}", templateId);
            } else {
                logger.info("Uploaded template to openFHIR Failed: {}", e);
            }
            return "Template upload to openFHIR error";
        }
    }
}
