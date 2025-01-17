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

    private String originalMessage;

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        // Unwrap the RuntimeCamelException if necessary
        if (ex instanceof RuntimeCamelException) {
            ex = (Exception) ex.getCause();
        }
        Assert.notNull(ex, "Exception must not be null");
        exchange.getIn().setBody(ex.getMessage());

        if (ex instanceof ResourceNotFoundException) {
            handleResourceNotFound((ResourceNotFoundException) ex);
        } else if (ex instanceof UnprocessableEntityException) {
            handleUnprocessableEntityException((UnprocessableEntityException) ex);
        } else if (ex instanceof InvalidRequestException) {
            handleInvalidRequestException((InvalidRequestException) ex);
        } else if (ex instanceof ForbiddenOperationException) {
            handleForbiddenOperationException((ForbiddenOperationException) ex);
        } else if (ex instanceof MethodNotAllowedException) {
            handleMethodNotAllowedException((MethodNotAllowedException) ex);
        } else if (ex instanceof NotImplementedOperationException) {
            handleNotImplementedOperationException((NotImplementedOperationException) ex);
        } else if (ex instanceof NotModifiedException) {
            handleNotModifiedException((NotModifiedException) ex);
        } else if (ex instanceof PayloadTooLargeException) {
            handlePayloadTooLargeException((PayloadTooLargeException) ex);
        } else if (ex instanceof PreconditionFailedException) {
            handlePreconditionFailedException((PreconditionFailedException) ex);
        } else if (ex instanceof ResourceGoneException) {
            handleResourceGoneException((ResourceGoneException) ex);
        } else if (ex instanceof ResourceVersionConflictException) {
            handleResourceVersionConflictException((ResourceVersionConflictException) ex);
        } else if (ex instanceof UnclassifiedServerFailureException) {
            handleUnclassifiedServerFailureException((UnclassifiedServerFailureException) ex);
        } else if (ex instanceof AuthenticationException) {
            handleAuthenticationException((AuthenticationException) ex);
        } else if (ex instanceof BaseServerResponseException) {
            handleBaseServerResponse((BaseServerResponseException) ex);
        }
        handleException(ex);
    }

    private void handleException(Exception ex) {
        throw new InternalErrorException("Error occurred while processing fhir bridge: " + ex.getMessage(), ex);
    }

    private void handleAuthenticationException(AuthenticationException ex) {
        throw new AuthenticationException("FHIR Server Exception: Authentication failed:", ex);
    }

    private void handleBaseServerResponse(BaseServerResponseException ex) {
        LOG.error("FHIR server error ({}): {}", ex.getStatusCode(), ex.getMessage());
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "FHIR server error: " + ex.getMessage(), "BaseServerResponseHandler");

        LOG.warn("FHIR server error: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new InternalErrorException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleResourceNotFound(ResourceNotFoundException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource not found: " + ex.getMessage(), "ResourceNotFoundHandler");

        LOG.warn("Resource not found: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ResourceNotFoundException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleUnprocessableEntityException(UnprocessableEntityException ex) {
        var outcome = new OperationOutcome();
        String baseMessage = originalMessage != null ? originalMessage : ex.getMessage();
        String diagnostics = baseMessage.startsWith("FHIR Server Exception: Validation failed: ")
                ? baseMessage
                : "FHIR Server Exception: Validation failed: " + baseMessage;

        if (originalMessage == null) {
            originalMessage = baseMessage;
        }


        ValidationUtils.addError(outcome, diagnostics, "UnprocessableEntityHandler");

        LOG.warn("Validation failed: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new UnprocessableEntityException(diagnostics, outcome);
        }
    }

    private void handleInvalidRequestException(InvalidRequestException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Invalid request: " + ex.getMessage(), "InvalidRequestHandler");

        LOG.warn("Invalid request: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new InvalidRequestException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleForbiddenOperationException(ForbiddenOperationException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Forbidden operation: " + ex.getMessage(), "ForbiddenOperationHandler");

        LOG.warn("Forbidden operation: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ForbiddenOperationException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleMethodNotAllowedException(MethodNotAllowedException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Method not allowed: " + ex.getMessage(), "MethodNotAllowedHandler");

        LOG.warn("Method not allowed: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new MethodNotAllowedException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleNotImplementedOperationException(NotImplementedOperationException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Operation not implemented: " + ex.getMessage(), "NotImplementedOperationHandler");

        LOG.warn("Operation not implemented: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new NotImplementedOperationException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleNotModifiedException(NotModifiedException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource not modified: " + ex.getMessage(), "NotModifiedHandler");

        LOG.warn("Resource not modified: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new NotModifiedException("FHIR Server Exception: ", outcome);
        }
    }

    private void handlePayloadTooLargeException(PayloadTooLargeException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Payload too large: " + ex.getMessage(), "PayloadTooLargeHandler");

        LOG.warn("Payload too large: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new PayloadTooLargeException("FHIR Server Exception: ", outcome);
        }
    }

    private void handlePreconditionFailedException(PreconditionFailedException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Precondition failed: " + ex.getMessage(), "PreconditionFailedHandler");

        LOG.warn("Precondition failed: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new PreconditionFailedException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleResourceGoneException(ResourceGoneException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource gone: " + ex.getMessage(), "ResourceGoneHandler");

        LOG.warn("Resource gone: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ResourceGoneException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleResourceVersionConflictException(ResourceVersionConflictException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Resource version conflict: " + ex.getMessage(), "ResourceVersionConflictHandler");

        LOG.warn("Resource version conflict: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new ResourceVersionConflictException("FHIR Server Exception: ", outcome);
        }
    }

    private void handleUnclassifiedServerFailureException(UnclassifiedServerFailureException ex) {
        var outcome = new OperationOutcome();
        ValidationUtils.addError(outcome, "Invalid request: " + ex.getMessage(), "UnclassifiedServerFailureHandler");

        LOG.warn("Unclassified server failure: {}", ex.getMessage());
        if (outcome.hasIssue()) {
            throw new InternalErrorException("FHIR Server Exception: ", outcome);
        }
    }
}
