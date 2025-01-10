package org.ehrbase.fhirbridge.exception;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.OpenEhrMappingExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import static org.ehcache.core.exceptions.StorePassThroughException.handleException;

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
        throw new InternalErrorException("Error occurred while openFHIR mapping conversion: " + ex.getMessage(), ex);
    }
}
