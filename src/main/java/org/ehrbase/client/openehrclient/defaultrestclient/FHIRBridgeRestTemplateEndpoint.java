package org.ehrbase.client.openehrclient.defaultrestclient;

import java.util.Optional;
import org.ehrbase.client.exception.ClientException;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FHIRBridgeRestTemplateEndpoint extends DefaultRestTemplateEndpoint  {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRestTemplateEndpoint.class);

    public static final String DEFINITION_TEMPLATE_ADL_1_4_PATH = "rest/openehr/v1/definition/template/adl1.4/";

    private final DefaultRestClient defaultRestClient;


    public FHIRBridgeRestTemplateEndpoint(DefaultRestClient defaultRestClient) {
        super(defaultRestClient);
        this.defaultRestClient = defaultRestClient;
    }

   
    @Override
    public void ensureExistence(String templateId) {
        Optional<OPERATIONALTEMPLATE> operationalTemplate = defaultRestClient.getTemplateProvider().find(templateId);
        if (!operationalTemplate.isPresent()) {
            throw new ClientException(String.format("Unknown Template with Id %s", templateId));
        }
        if (!findTemplate(templateId).isPresent()) {
            super.upload(operationalTemplate.get());
            logger.info("Uploaded template to openEHR: {}", templateId);
        } else {
            logger.info("Template already exists in openEHR: {}", templateId);
        }
    }

}