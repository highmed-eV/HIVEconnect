package org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient;

import javax.xml.namespace.QName;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.xmlbeans.XmlOptions;

import org.ehrbase.openehr.sdk.util.exception.ClientException;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HIVEconnectRestTemplateEndpoint extends DefaultRestTemplateEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(HIVEconnectRestTemplateEndpoint.class);

    private final DefaultRestClient defaultRestClient;


    public HIVEconnectRestTemplateEndpoint(DefaultRestClient defaultRestClient) {
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
            this.upload(operationalTemplate.get());
            logger.info("Uploaded template to openEHR: {}", templateId);
        } else {
            logger.info("Template already exists in openEHR: {}", templateId);
        }
    }

    @Override
    String upload(OPERATIONALTEMPLATE operationaltemplate){
        URI uri = this.defaultRestClient.getConfig().getBaseUri().resolve("rest/openehr/v1/definition/template/adl1.4/");
        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
        ContentType contentType = ContentType.create("application/xml", StandardCharsets.UTF_8);
        HttpResponse response = this.defaultRestClient.internalPost(uri, (Map)null, operationaltemplate.xmlText(opts), contentType, ContentType.APPLICATION_XML.getMimeType());
        return (String)Optional.ofNullable(response.getFirstHeader("ETag")).map((header) -> {
            return StringUtils.unwrap(StringUtils.removeStart(header.getValue(), "W/"), '"');
        }).orElse(operationaltemplate.getTemplateId().getValue());
    }

}