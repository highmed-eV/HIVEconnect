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

package org.highmed.hiveconnect.camel.processor;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.ehrbase.openehr.sdk.util.exception.WrongStatusCodeException;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * {@link Processor} that handles exceptions thrown by Camel routes.
 *
 * @since 1.0.0
 */
@Component(DefaultExceptionHandler.BEAN_ID)
@SuppressWarnings("java:S6212")
public class DefaultExceptionHandler implements Processor, MessageSourceAware {

    public static final String BEAN_ID = "defaultExceptionHandler";

    private MessageSourceAccessor messages;

    @Override
    public void process(Exchange exchange) {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        if (ex instanceof WrongStatusCodeException e) {
            handleWrongStatusCode(e);
        } else {
            handleException(ex);
        }
    }

    private void handleWrongStatusCode(WrongStatusCodeException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getActualStatusCode());
        throw new UnprocessableEntityException(messages.getMessage("ehrbase.wrongStatusCode",
                new Object[]{status.value(), status.getReasonPhrase(), ex.getMessage()}), ex);
    }

    private void handleException(Exception ex) {
        throw new InternalErrorException(ex.getMessage());
    }

    @Override
    public void setMessageSource(@NonNull MessageSource messageSource) {
        messages = new MessageSourceAccessor(messageSource);
    }
}
