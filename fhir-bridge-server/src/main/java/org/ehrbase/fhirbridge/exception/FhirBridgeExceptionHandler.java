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

package org.ehrbase.fhirbridge.exception;

import ca.uhn.fhir.rest.server.exceptions.*;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.ehrbase.fhirbridge.fhir.validation.ValidationUtils;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * {@link org.apache.camel.Processor Processor} that transforms an exception
 * thrown by the {@link org.ehrbase.fhirbridge.camel.route FhirBridgeRouteBuilder, FhirRouteBuilder} into the corresponding FHIR exception.
 *
 * @since 1.2.0
 */
public class FhirBridgeExceptionHandler  implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(FhirBridgeExceptionHandler.class);
    public static final String FHIR_SERVER_EXCEPTION = "FHIR Server Exception: ";

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
        String errorMessage =  "Internal Error occurred while processing FHIR Bridge: " + ex.getMessage();;
        throw new InternalErrorException(errorMessage);        
    }

    private void handleAuthenticationException(AuthenticationException ex) {
        throw new AuthenticationException("FHIR Bridge Server Exception: Authentication failed:", ex);
    }

    private void handleBaseServerResponse(BaseServerResponseException ex) {
        String errorMessage = "FHIR Bridge server error: " + ex.getMessage();
        throw new InternalErrorException(errorMessage);
    }

    private void handleResourceNotFound(ResourceNotFoundException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource not found: " + ex.getMessage(), "ResourceNotFoundHandler");

        LOG.warn("Resource not found: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ResourceNotFoundException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleUnprocessableEntityException(UnprocessableEntityException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Validation failed: " + ex.getMessage(), "UnprocessableEntityExceptionHandler");

        LOG.warn("Validation failed: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new UnprocessableEntityException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleInvalidRequestException(InvalidRequestException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Invalid request: " + ex.getMessage(), "InvalidRequestHandler");

        LOG.warn("Invalid request: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new InvalidRequestException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleForbiddenOperationException(ForbiddenOperationException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Forbidden operation: " + ex.getMessage(), "ForbiddenOperationHandler");

        LOG.warn("Forbidden operation: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ForbiddenOperationException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleMethodNotAllowedException(MethodNotAllowedException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Method not allowed: " + ex.getMessage(), "MethodNotAllowedHandler");

        LOG.warn("Method not allowed: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new MethodNotAllowedException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleNotImplementedOperationException(NotImplementedOperationException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Operation not implemented: " + ex.getMessage(), "NotImplementedOperationHandler");

        LOG.warn("Operation not implemented: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new NotImplementedOperationException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleNotModifiedException(NotModifiedException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource not modified: " + ex.getMessage(), "NotModifiedHandler");

        LOG.warn("Resource not modified: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new NotModifiedException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handlePayloadTooLargeException(PayloadTooLargeException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Payload too large: " + ex.getMessage(), "PayloadTooLargeHandler");

        LOG.warn("Payload too large: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new PayloadTooLargeException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handlePreconditionFailedException(PreconditionFailedException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Precondition failed: " + ex.getMessage(), "PreconditionFailedHandler");

        LOG.warn("Precondition failed: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new PreconditionFailedException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleResourceGoneException(ResourceGoneException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource gone: " + ex.getMessage(), "ResourceGoneHandler");

        LOG.warn("Resource gone: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ResourceGoneException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleResourceVersionConflictException(ResourceVersionConflictException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource version conflict: " + ex.getMessage(), "ResourceVersionConflictHandler");

        LOG.warn("Resource version conflict: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ResourceVersionConflictException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }

    private void handleUnclassifiedServerFailureException(UnclassifiedServerFailureException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Invalid request: " + ex.getMessage(), "UnclassifiedServerFailureHandler");

        LOG.warn("Unclassified server failure: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new InternalErrorException(FHIR_SERVER_EXCEPTION, outcome);
        }
    }
}
