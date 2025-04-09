package org.ehrbase.fhirbridge.camel.component.ehr.composition;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.ehrbase.fhirbridge.camel.component.ehr.EhrConfiguration;
import org.ehrbase.fhirbridge.config.DebugProperties;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClient;

@UriEndpoint(firstVersion = "1.0.0", scheme = "ehr-composition", title = "EHR Composition", syntax = "ehr-composition:name", producerOnly = true)
@SuppressWarnings({"java:S2160", "java:S1452"})
@Setter
@Getter
public class CompositionEndpoint extends DefaultEndpoint {

    @UriPath
    private String name;

    @UriParam
    private CompositionOperation operation;

    private Class<?> expectedType;

    @UriParam
    private EhrConfiguration configuration;

    private DebugProperties properties;

    public CompositionEndpoint(String uri, CompositionComponent component, EhrConfiguration configuration) {
        super(uri, component);
        this.configuration = configuration;
    }

    @Override
    public Producer createProducer() {
        return new CompositionProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException("Cannot consume from Composition endpoint");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public OpenEhrClient getOpenEhrClient() {
        return getConfiguration().getOpenEhrClient();
    }

    public void setOpenEhrClient(OpenEhrClient openEhrClient) {
        getConfiguration().setOpenEhrClient(openEhrClient);
    }
}
