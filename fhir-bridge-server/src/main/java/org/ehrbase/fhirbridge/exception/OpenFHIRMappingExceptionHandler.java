package org.ehrbase.fhirbridge.exception;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.ehrbase.fhirbridge.camel.processor.OpenEhrMappingExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * {@link org.apache.camel.Processor Processor} that transforms an exception
 * thrown by the {@link org.ehrbase.fhirbridge.openfhir.openfhirclient OpenFHIRAdapter} into the corresponding OpenFHIR exception.
 *
 * @since 1.2.0
 */
public class OpenFHIRMappingExceptionHandler implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenEhrMappingExceptionHandler.class);

    @Override
    public void process(Exchange exchange) {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Assert.notNull(ex, "Exception must not be null");
        exchange.getIn().setBody(ex.getMessage());
        handleException(ex);
    }

    private void handleException(Exception ex) {
        throw new RuntimeException("Error occurred while openFHIR mapping conversion: " + ex.getMessage(), ex);
    }
}
