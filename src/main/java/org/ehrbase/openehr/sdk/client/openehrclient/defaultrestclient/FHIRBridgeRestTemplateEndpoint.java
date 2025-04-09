package org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient;

import java.util.Optional;
import org.ehrbase.openehr.sdk.util.exception.ClientException;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FHIRBridgeRestTemplateEndpoint extends DefaultRestTemplateEndpoint  {

    private static final Logger logger = LoggerFactory.getLogger(FHIRBridgeRestTemplateEndpoint.class);

    private final DefaultRestClient defaultRestClient;


    public FHIRBridgeRestTemplateEndpoint(DefaultRestClient defaultRestClient) {
        super(defaultRestClient);
        this.defaultRestClient = defaultRestClient;
    }

   
    @Override
    public void ensureExistence(String templateId) {
        Optional<OPERATIONALTEMPLATE> operationalTemplate = defaultRestClient.getTemplateProvider().find(templateId);
        if (operationalTemplate.isEmpty()) {
            throw new ClientException(String.format("Unknown Template with Id %s", templateId));
        }
        if (findTemplate(templateId).isEmpty()) {
            super.upload(operationalTemplate.get());
            logger.info("Uploaded template to openEHR: {}", templateId);
        } else {
            logger.info("Template already exists in openEHR: {}", templateId);
        }
    }

}