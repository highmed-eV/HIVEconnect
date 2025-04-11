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

package org.ehrbase.fhirbridge.fhir.condition;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConditionResourceProvider implements IResourceProvider  {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public Class<Condition> getResourceType() {
        return Condition.class;
    }
    
    @Create
    public MethodOutcome create(@ResourceParam Condition condition,
                                RequestDetails requestDetails,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        log.info("Executing 'Provide Condition' transaction using 'create' operation...");
    
        // Validate the incoming resource
        if (condition == null || !condition.hasSubject() || !condition.hasCode()) {
            throw new UnprocessableEntityException("Condition resource must have a subject and a code.");
        }

        try {
            // Call Camel route with the Condition resource
            return producerTemplate.requestBody("direct:CreateRouteProcess", condition, MethodOutcome.class);
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

    @Search(type = Condition.class)
    public Condition searchCondition(@OptionalParam(name = IAnyResource.SP_RES_ID) TokenAndListParam id,
                                           @OptionalParam(name = Constants.PARAM_LANGUAGE) StringAndListParam language,
                                           @OptionalParam(name = Constants.PARAM_LASTUPDATED) DateRangeParam lastUpdated,
                                           @OptionalParam(name = Constants.PARAM_PROFILE) UriAndListParam profile,
                                           @OptionalParam(name = Constants.PARAM_SOURCE) UriAndListParam source,
                                           @OptionalParam(name = Constants.PARAM_SECURITY) TokenAndListParam security,
                                           @OptionalParam(name = Constants.PARAM_TAG) TokenAndListParam tag,
                                           @OptionalParam(name = Constants.PARAM_CONTENT) StringAndListParam content,
                                           @OptionalParam(name = Constants.PARAM_TEXT) StringAndListParam text,
                                           @OptionalParam(name = Constants.PARAM_FILTER) StringAndListParam filter,
                                           @OptionalParam(name = Condition.SP_ABATEMENT_AGE) QuantityAndListParam abatementAge,
                                           @OptionalParam(name = Condition.SP_ABATEMENT_DATE) DateRangeParam abatementDate,
                                           @OptionalParam(name = Condition.SP_ABATEMENT_STRING) StringAndListParam abatementString,
                                           @OptionalParam(name = Condition.SP_ASSERTER) ReferenceAndListParam asserter,
                                           @OptionalParam(name = Condition.SP_BODY_SITE) TokenAndListParam bodySite,
                                           @OptionalParam(name = Condition.SP_CATEGORY) TokenAndListParam category,
                                           @OptionalParam(name = Condition.SP_CLINICAL_STATUS) TokenAndListParam clinicalStatus,
                                           @OptionalParam(name = Condition.SP_CODE) TokenAndListParam code,
                                           @OptionalParam(name = Condition.SP_ENCOUNTER) ReferenceAndListParam encounter,
                                           @OptionalParam(name = Condition.SP_EVIDENCE) TokenAndListParam evidence,
                                           @OptionalParam(name = Condition.SP_EVIDENCE_DETAIL) ReferenceAndListParam evidenceDetail,
                                           @OptionalParam(name = Condition.SP_IDENTIFIER) TokenAndListParam identifier,
                                           @OptionalParam(name = Condition.SP_ONSET_AGE) QuantityAndListParam onsetAge,
                                           @OptionalParam(name = Condition.SP_ONSET_DATE) DateRangeParam onsetDate,
                                           @OptionalParam(name = Condition.SP_ONSET_INFO) StringAndListParam onsetInfo,
                                           @OptionalParam(name = Condition.SP_PATIENT) ReferenceAndListParam patient,
                                           @OptionalParam(name = Condition.SP_RECORDED_DATE) DateRangeParam recordedDate,
                                           @OptionalParam(name = Condition.SP_SEVERITY) TokenAndListParam severity,
                                           @OptionalParam(name = Condition.SP_STAGE) TokenAndListParam stage,
                                           @OptionalParam(name = Condition.SP_SUBJECT) ReferenceAndListParam subject,
                                           @OptionalParam(name = Condition.SP_VERIFICATION_STATUS) TokenAndListParam verificationStatus,
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

        searchParams.add(Condition.SP_ABATEMENT_AGE, abatementAge);
        searchParams.add(Condition.SP_ABATEMENT_DATE, abatementDate);
        searchParams.add(Condition.SP_ABATEMENT_STRING, abatementString);
        searchParams.add(Condition.SP_ASSERTER, asserter);
        searchParams.add(Condition.SP_BODY_SITE, bodySite);
        searchParams.add(Condition.SP_CATEGORY, category);
        searchParams.add(Condition.SP_CLINICAL_STATUS, clinicalStatus);
        searchParams.add(Condition.SP_CODE, code);
        searchParams.add(Condition.SP_ENCOUNTER, encounter);
        searchParams.add(Condition.SP_EVIDENCE, evidence);
        searchParams.add(Condition.SP_EVIDENCE_DETAIL, evidenceDetail);
        searchParams.add(Condition.SP_IDENTIFIER, identifier);
        searchParams.add(Condition.SP_ONSET_AGE, onsetAge);
        searchParams.add(Condition.SP_ONSET_DATE, onsetDate);
        searchParams.add(Condition.SP_ONSET_INFO, onsetInfo);
        searchParams.add(Condition.SP_PATIENT, patient);
        searchParams.add(Condition.SP_RECORDED_DATE, recordedDate);
        searchParams.add(Condition.SP_SEVERITY, severity);
        searchParams.add(Condition.SP_STAGE, stage);
        searchParams.add(Condition.SP_SUBJECT, subject);
        searchParams.add(Condition.SP_VERIFICATION_STATUS, verificationStatus);

        searchParams.setLastUpdated(lastUpdated);
        searchParams.setCount(count);
        searchParams.setOffset(offset);
        searchParams.setSort(sort);
        // Call Camel route with the Condition resource
        return producerTemplate.requestBody("direct:SearchRouteProcess", requestDetails, Condition.class);
    }

    @Read(version = true)
    public Condition readCondition(@IdParam IdType id, RequestDetails requestDetails,
                                   HttpServletRequest request, HttpServletResponse response) {
        // Call Camel route with the Condition resource
        return producerTemplate.requestBody("direct:CreateRouteProcessRoute", requestDetails, Condition.class);
    }
}


