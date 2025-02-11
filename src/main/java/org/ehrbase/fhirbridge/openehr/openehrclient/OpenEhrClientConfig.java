package org.ehrbase.fhirbridge.openehr.openehrclient;

import java.net.URI;
// import org.ehrbase.client.flattener.DefaultValuesProvider;

public class OpenEhrClientConfig {

  private final URI baseUri;
  private CompositionFormat compositionFormat = CompositionFormat.JSON;
//   private DefaultValuesProvider defaultValuesProvider;

  public OpenEhrClientConfig(URI baseUri) {
    this.baseUri = baseUri;
  }

  public URI getBaseUri() {
    return baseUri;
  }

  public CompositionFormat getCompositionFormat() {
    return compositionFormat;
  }

  public void setCompositionFormat(CompositionFormat compositionFormat) {
    this.compositionFormat = compositionFormat;
  }

//   public DefaultValuesProvider getDefaultValuesProvider() {
//     return defaultValuesProvider;
//   }

//   public void setDefaultValuesProvider(DefaultValuesProvider defaultValuesProvider) {
//     this.defaultValuesProvider = defaultValuesProvider;
//   }
}
