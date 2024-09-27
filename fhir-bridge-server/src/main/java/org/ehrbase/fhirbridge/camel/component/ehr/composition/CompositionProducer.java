package org.ehrbase.fhirbridge.camel.component.ehr.composition;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.apache.commons.io.FileUtils;
import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CompositionProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(CompositionProducer.class);

    private final CompositionEndpoint endpoint;

    public CompositionProducer(CompositionEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) {
        UUID ehrId = exchange.getIn().getHeader(CompositionConstants.EHR_ID, UUID.class);
        if (ehrId == null) {
            throw new IllegalArgumentException("EhrId must not be null");
        }

        CompositionOperation operation = determineOperation(exchange);
        if (operation == CompositionOperation.mergeCompositionEntity) {
            mergeCompositionEntity(ehrId, exchange);
        } else if (operation == CompositionOperation.find) {
            find(ehrId, exchange);
        } else {
            throw new IllegalArgumentException("Unsupported operation");
        }
    }

    private void mergeCompositionEntity(UUID ehrId, Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body == null) {
            throw new IllegalArgumentException("Body must not be null");
        }

        if (endpoint.getProperties().isEnabled()) {
            debugMapping((Composition) body);
        }

        //This internally checks if versionUID is present
        //if yes: post and return the mergedCompositions versionUid
        //else put and return the mergedCompositions versionUid
        Object mergedComposition = endpoint.getOpenEhrClient().compositionEndpoint(ehrId).mergeCompositionEntity(body);
        exchange.getMessage().setHeader(CompositionConstants.VERSION_UID, ((Composition) mergedComposition));

        exchange.getMessage().setBody(mergedComposition);
    }

    private void debugMapping(Composition composition) {
        CanonicalJson canonicalJson = new CanonicalJson();
        String compositionJson = canonicalJson.marshal(composition);
        writeToFile(compositionJson);
    }

    private void writeToFile(String compositionJson) {
        File output = new File(endpoint.getProperties().getMappingOutputDirectory() + "/mapping-output.json");
        try {
            FileUtils.writeStringToFile(output, compositionJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("An I/O exception occurred while writing the composition in the output file", e);
        }
    }

    private void find(UUID ehrId, Exchange exchange) {
        UUID compositionId = exchange.getIn().getHeader(CompositionConstants.COMPOSITION_ID, UUID.class);
        Class<?> expectedType = endpoint.getExpectedType();

        Object result = endpoint.getOpenEhrClient().compositionEndpoint(ehrId)
                .find(compositionId, expectedType)
                .orElse(null);
        exchange.getMessage().setBody(result);
    }

    private CompositionOperation determineOperation(Exchange exchange) {
        CompositionOperation operation = exchange.getIn().getHeader(CompositionConstants.OPERATION, CompositionOperation.class);
        if (operation == null) {
            operation = endpoint.getOperation();
        }
        return operation;
    }
}
