package org.ehrbase.fhirbridge.openehr.openehrclient;

import org.ehrbase.client.exception.ClientException;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.ehrbase.response.openehr.TemplatesResponseData;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.Optional;

public interface TemplateEndpoint {

    /**
     * Find a template by templateId
     *
     * @param templateId
     * @return {@link OPERATIONALTEMPLATE}
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    Optional<OPERATIONALTEMPLATE> findTemplate(String templateId);

    /**
     * Retrieves a list of templates
     *
     * @return
     */
    TemplatesResponseData findAllTemplates();

    /**
     * Ensure that the Template with {@code templateId} exists in the remote system.
     *
     * @param templateId Id of the template to check
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    void ensureExistence(String templateId);


}
