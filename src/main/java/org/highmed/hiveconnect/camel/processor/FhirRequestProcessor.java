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

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.2.0
 */
public interface FhirRequestProcessor extends Processor {
    Logger LOG = LoggerFactory.getLogger(FhirRequestProcessor.class);
    /**
     * Returns the current request details.
     *
     * @param exchange the current exchange
     * @return the request details
     */
    default RequestDetails getRequestDetails(Exchange exchange) {
        exchange.getIn().getHeaders().forEach((key, value) -> LOG.info("Key: {} {}", key, value));

        if (ObjectHelper.isEmpty(exchange.getIn().getHeader("FHIR_REQUEST_DETAILS"))) {
            throw new IllegalArgumentException("RequestDetails must not be null");
        }
        return exchange.getIn().getHeader("FHIR_REQUEST_DETAILS", RequestDetails.class);
    }
}
