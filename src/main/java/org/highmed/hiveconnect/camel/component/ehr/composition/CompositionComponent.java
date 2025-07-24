package org.highmed.hiveconnect.camel.component.ehr.composition;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.highmed.hiveconnect.camel.component.ehr.EhrConfiguration;
import org.highmed.hiveconnect.config.DebugProperties;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClient;

import java.util.Map;
import java.util.Set;

/**
 * Composition component
 */
@Getter
@Setter
@Slf4j
public class CompositionComponent extends DefaultComponent {

    private EhrConfiguration configuration;

    private boolean allowAutoWiredOpenEhrClient = true;

    public CompositionComponent() {
        this.configuration = new EhrConfiguration();
    }

    public CompositionComponent(CamelContext context) {
        super(context);
        this.configuration = new EhrConfiguration();
    }

    @Override
    protected void doInit() throws Exception {
        if (configuration.getOpenEhrClient() == null && isAllowAutoWiredOpenEhrClient()) {
            Set<OpenEhrClient> beans = getCamelContext().getRegistry().findByType(OpenEhrClient.class);
            if (beans.size() == 1) {
                OpenEhrClient client = beans.iterator().next();
                configuration.setOpenEhrClient(client);
            } else if (beans.size() > 1) {
                log.debug("Cannot autowire OpenEhrClient as {} instances found in registry.", beans.size());
            }
        }
        super.doStart();
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        DebugProperties properties = getCamelContext().getRegistry().lookupByNameAndType("debugProperties", DebugProperties.class);
        final EhrConfiguration newConfiguration = configuration.copy();
        CompositionEndpoint endpoint = new CompositionEndpoint(uri, this, newConfiguration);
        endpoint.setProperties(properties);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    public OpenEhrClient getOpenEhrClient() {
        return configuration.getOpenEhrClient();
    }
}
