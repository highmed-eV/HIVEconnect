package org.ehrbase.fhirbridge.exception;

import org.apache.camel.builder.DefaultErrorHandlerBuilder;

public class CamelErrorHandlerBuilder extends DefaultErrorHandlerBuilder {

    public CamelErrorHandlerBuilder() {
        // Set custom error handling logic
        this.maximumRedeliveries(3)
            .redeliveryDelay(2000) // Retry 3 times with 2s delay
            .onExceptionOccurred(exchange -> {
                // Log the exception
                System.out.println("Error occurred: " + exchange.getException().getMessage());
            });
    }
}
