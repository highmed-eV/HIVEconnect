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

package org.highmed.hiveconnect.fhir.bundle;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BundleResourceProvider implements IResourceProvider  {

    private final ProducerTemplate producerTemplate;

    public BundleResourceProvider(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @Override
    public Class<Bundle> getResourceType() {
        return Bundle.class;
    }
    
    @Create
    public MethodOutcome create(@ResourceParam Bundle bundle,
                                RequestDetails requestDetails,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        log.info("Executing 'Provide Bundle' transaction using 'create' operation...");
    
        MethodOutcome methodOutcome;

        
        try {
            // Call Camel route with the Bundle resource
            methodOutcome = producerTemplate.requestBodyAndHeader("direct:CreateRouteProcess", requestDetails, Exchange.HTTP_METHOD, "POST", MethodOutcome.class);
            return methodOutcome;
        } catch (CamelExecutionException exception) {
            Exchange exchange = exception.getExchange();
            if (exchange.isFailed()) {
                BaseServerResponseException baseException = exchange.getException(BaseServerResponseException.class);
                throw (baseException != null) ? baseException : new InternalErrorException("Unexpected server error", exchange.getException());
            } else {
                throw new InternalErrorException("Unexpected internal server error", exchange.getException());
            }
        }
    }


    @Update
    public MethodOutcome update(@IdParam IdType bundleId,
                                @ResourceParam Bundle bundle,
                                RequestDetails requestDetails,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        log.info("Executing 'Update Bundle' transaction using 'create' operation...");
    
        MethodOutcome methodOutcome;
        try {
            // Call Camel route with the Bundle resource
            methodOutcome = producerTemplate.requestBodyAndHeader("direct:CreateRouteProcess", requestDetails, Exchange.HTTP_METHOD, "PUT", MethodOutcome.class);
            return methodOutcome;
        } catch (CamelExecutionException exception) {
            Exchange exchange = exception.getExchange();
            if (exchange.isFailed()) {
                BaseServerResponseException baseException = exchange.getException(BaseServerResponseException.class);
                throw (baseException != null) ? baseException : new InternalErrorException("Unexpected server error", exchange.getException());
            } else {
                throw new InternalErrorException("Unexpected internal server error", exchange.getException());
            }
        }
        
    }


    @Search(type = Bundle.class)
    public Bundle searchBundle(@OptionalParam(name = IAnyResource.SP_RES_ID) TokenAndListParam id,
                                           @OptionalParam(name = Constants.PARAM_LANGUAGE) StringAndListParam language,
                                           @OptionalParam(name = Constants.PARAM_LASTUPDATED) DateRangeParam lastUpdated,
                                           @OptionalParam(name = Constants.PARAM_PROFILE) UriAndListParam profile,
                                           @OptionalParam(name = Constants.PARAM_SOURCE) UriAndListParam source,
                                           @OptionalParam(name = Constants.PARAM_SECURITY) TokenAndListParam security,
                                           @OptionalParam(name = Constants.PARAM_TAG) TokenAndListParam tag,
                                           @OptionalParam(name = Constants.PARAM_CONTENT) StringAndListParam content,
                                           @OptionalParam(name = Constants.PARAM_TEXT) StringAndListParam text,
                                           @OptionalParam(name = Constants.PARAM_FILTER) StringAndListParam filter,
                                           @OptionalParam(name = Bundle.SP_IDENTIFIER) TokenAndListParam identifier,
                                           @Count Integer count, @Offset Integer offset, @Sort SortSpec sort,
                                           RequestDetails requestDetails, HttpServletRequest request, HttpServletResponse response) {

        SearchParameterMap searchParams = new SearchParameterMap();
        searchParams.add(IAnyResource.SP_RES_ID, id);
        searchParams.add(Constants.PARAM_LANGUAGE, language);

        searchParams.add(Constants.PARAM_PROFILE, profile);
        searchParams.add(Constants.PARAM_SOURCE, source);
        searchParams.add(Constants.PARAM_SECURITY, security);
        searchParams.add(Constants.PARAM_TAG, tag);
        searchParams.add(Constants.PARAM_CONTENT, content);
        searchParams.add(Constants.PARAM_TEXT, text);
        searchParams.add(Constants.PARAM_FILTER, filter);

        searchParams.add(Bundle.SP_IDENTIFIER, identifier);

        searchParams.setLastUpdated(lastUpdated);
        searchParams.setCount(count);
        searchParams.setOffset(offset);
        searchParams.setSort(sort);
        // Call Camel route with the Bundle resource
        return producerTemplate.requestBody("direct:SearchRouteProcess", requestDetails, Bundle.class);
    }

    @Read(version = true)
    public Bundle readBundle(@IdParam IdType id, RequestDetails requestDetails,
                                   HttpServletRequest request, HttpServletResponse response) {
        // Call Camel route with the Bundle resource

        return producerTemplate.requestBody("direct:CreateRouteProcessRoute", requestDetails, Bundle.class);
    }
}


