/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.highmed.hiveconnect.exception;

import ca.uhn.fhir.rest.server.exceptions.*;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * {@link org.apache.camel.Processor Processor} that transforms an exception
 * thrown by the {@link org.highmed.hiveconnect.camel.route HiveConnectRouteBuilder, FhirRouteBuilder} into the corresponding FHIR exception.
 *
 * @since 1.2.0
 */
public class HiveConnectExceptionHandler implements Processor {
    private static final Logger log = LoggerFactory.getLogger(HiveConnectExceptionHandler.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        // Unwrap the RuntimeCamelException if necessary
        if (ex instanceof RuntimeCamelException) {
            ex = (Exception) ex.getCause();
        }
        Assert.notNull(ex, "Exception must not be null");
        exchange.getIn().setBody(ex.getMessage());

        if (ex instanceof ResourceNotFoundException e) {
            handleResourceNotFound(e);
        } else if (ex instanceof UnprocessableEntityException e) {
            handleUnprocessableEntityException(e);
        } else if (ex instanceof InvalidRequestException e) {
            handleInvalidRequestException(e);
        } else if (ex instanceof ForbiddenOperationException e) {
            handleForbiddenOperationException(e);
        } else if (ex instanceof MethodNotAllowedException e) {
            handleMethodNotAllowedException(e);
        } else if (ex instanceof NotImplementedOperationException e) {
            handleNotImplementedOperationException(e);
        } else if (ex instanceof NotModifiedException e) {
            handleNotModifiedException(e);
        } else if (ex instanceof PayloadTooLargeException e) {
            handlePayloadTooLargeException(e);
        } else if (ex instanceof PreconditionFailedException e) {
            handlePreconditionFailedException(e);
        } else if (ex instanceof ResourceGoneException e) {
            handleResourceGoneException(e);
        } else if (ex instanceof ResourceVersionConflictException e) {
            handleResourceVersionConflictException(e);
        } else if (ex instanceof UnclassifiedServerFailureException e) {
            handleUnclassifiedServerFailureException(e);
        } else if (ex instanceof AuthenticationException e) {
            handleAuthenticationException(e);
        } else if (ex instanceof BaseServerResponseException e) {
            handleBaseServerResponse(e);
        }
        handleException(ex);
    }

    private void handleException(Exception ex) {
        String errorMessage =  "Internal Error occurred while processing FHIR Bridge: " + ex.getMessage();
        log.error(errorMessage);
        throw new InternalErrorException(errorMessage);
    }

    private void handleAuthenticationException(AuthenticationException ex) {
        throw new AuthenticationException("FHIR Bridge Server Exception: Authentication failed:", ex);
    }

    private void handleBaseServerResponse(BaseServerResponseException ex) {
        String errorMessage = "FHIR Bridge server error: " + ex.getMessage();
        log.error(errorMessage);
        throw new InternalErrorException(errorMessage);
    }

    private void handleResourceNotFound(ResourceNotFoundException ex) {
        String errorMessage = "Resource not found: " + ex.getMessage();
        log.error(errorMessage);
        throw new ResourceNotFoundException(errorMessage);
    }

    private void handleUnprocessableEntityException(UnprocessableEntityException ex) {
        String errorMessage = "Validation failed: " + ex.getMessage();
        log.error(errorMessage);
        throw new UnprocessableEntityException(errorMessage);
    }

    private void handleInvalidRequestException(InvalidRequestException ex) {
        String errorMessage =  "Invalid request: " + ex.getMessage();
        log.error(errorMessage);
        throw new InvalidRequestException(errorMessage);
    }

    private void handleForbiddenOperationException(ForbiddenOperationException ex) {
        String errorMessage = "Forbidden operation: " + ex.getMessage();
        log.error(errorMessage);
        throw new ForbiddenOperationException(errorMessage);
    }

    private void handleMethodNotAllowedException(MethodNotAllowedException ex) {
        String errorMessage =  "Method not allowed: " + ex.getMessage();
        log.error(errorMessage);
        throw new MethodNotAllowedException(errorMessage);
    }

    private void handleNotImplementedOperationException(NotImplementedOperationException ex) {
        String errorMessage =  "Operation not implemented: " + ex.getMessage();
        log.error(errorMessage);
        throw new NotImplementedOperationException(errorMessage);
    }

    private void handleNotModifiedException(NotModifiedException ex) {
        String errorMessage =   "Resource not modified: " + ex.getMessage();
        log.error(errorMessage);
        throw new NotModifiedException(errorMessage);
    }

    private void handlePayloadTooLargeException(PayloadTooLargeException ex) {
        String errorMessage = "Payload too large: " + ex.getMessage();
        log.error(errorMessage);
        throw new PayloadTooLargeException(errorMessage);
    }

    private void handlePreconditionFailedException(PreconditionFailedException ex) {
        String errorMessage = "Precondition failed: " + ex.getMessage();
        log.error(errorMessage);
        throw new PreconditionFailedException(errorMessage);
    }

    private void handleResourceGoneException(ResourceGoneException ex) {
        String errorMessage = "Resource gone: " + ex.getMessage();
        log.error(errorMessage);
        throw new ResourceGoneException(errorMessage);
    }

    private void handleResourceVersionConflictException(ResourceVersionConflictException ex) {
        String errorMessage = "Resource version conflict: " + ex.getMessage();
        log.error(errorMessage);
        throw new ResourceVersionConflictException(errorMessage);
    }

    private void handleUnclassifiedServerFailureException(UnclassifiedServerFailureException ex) {
        String errorMessage = "Unclassified server failure: " + ex.getMessage();
        log.error(errorMessage);
        throw new InternalErrorException(errorMessage);
    }
}
