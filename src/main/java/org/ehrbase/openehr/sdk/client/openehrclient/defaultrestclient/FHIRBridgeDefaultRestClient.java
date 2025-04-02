package org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient;


import org.apache.http.client.HttpClient;
import java.util.UUID;

import org.ehrbase.openehr.sdk.client.openehrclient.CompositionEndpoint;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.TemplateEndpoint;
import org.ehrbase.openehr.sdk.webtemplate.templateprovider.TemplateProvider;

public class FHIRBridgeDefaultRestClient extends DefaultRestClient {

  public FHIRBridgeDefaultRestClient(OpenEhrClientConfig config, TemplateProvider templateProvider) {
    super(config, templateProvider);
  }

  public FHIRBridgeDefaultRestClient(OpenEhrClientConfig config) {
    super(config, null, null);
  }

  public FHIRBridgeDefaultRestClient(
      OpenEhrClientConfig config, TemplateProvider templateProvider, HttpClient httpClient) {
    super(config, templateProvider, httpClient);
  }

 @Override
  public CompositionEndpoint compositionEndpoint(UUID ehrId) {
    return new FHIRBridgeRestCompositionEndpoint(this, ehrId);
  }
  
 @Override
  public TemplateEndpoint templateEndpoint() {
    return new FHIRBridgeRestTemplateEndpoint(this);
  }     
}
