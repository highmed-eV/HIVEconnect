package org.ehrbase.fhirbridge.exception;

import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelErrorHandlerBuilder extends DefaultErrorHandlerBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(CamelErrorHandlerBuilder.class);

    public CamelErrorHandlerBuilder() {
        // Set custom error handling logic
        this.maximumRedeliveries(3)
            .redeliveryDelay(2000) // Retry 3 times with 2s delay
            .onExceptionOccurred(exchange ->
                // Log the exception
                LOG.error("Error occurred: {}",exchange.getException().getMessage())
            );
    }
}
