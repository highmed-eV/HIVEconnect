package org.ehrbase.fhirbridge.openehr.openehrclient;

import java.util.Optional;
import org.ehrbase.webtemplate.templateprovider.TemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public class ClientTemplateProvider implements TemplateProvider {

  private DefaultRestClient restClient;

  public ClientTemplateProvider(DefaultRestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public Optional<OPERATIONALTEMPLATE> find(String templateId) {
    return restClient.templateEndpoint().findTemplate(templateId);
  }

}
