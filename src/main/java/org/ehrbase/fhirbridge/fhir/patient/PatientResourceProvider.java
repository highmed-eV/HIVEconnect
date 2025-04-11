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

package org.ehrbase.fhirbridge.fhir.patient;

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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PatientResourceProvider implements IResourceProvider  {

    private final ProducerTemplate producerTemplate;

    public PatientResourceProvider(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }
    
    @Create
    public MethodOutcome create(@ResourceParam Patient patient,
                                RequestDetails requestDetails,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        log.info("Executing 'Provide Patient' transaction using 'create' operation...");
    
        try {
            // Call Camel route with the Patient resource
            return producerTemplate.requestBodyAndHeader("direct:CreateRouteProcess", requestDetails, Exchange.HTTP_METHOD, "POST", MethodOutcome.class);
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

    @Search(type = Patient.class)
    public Patient searchPatient(@OptionalParam(name = IAnyResource.SP_RES_ID) TokenAndListParam id,
                                           @OptionalParam(name = Constants.PARAM_LANGUAGE) StringAndListParam language,
                                           @OptionalParam(name = Constants.PARAM_LASTUPDATED) DateRangeParam lastUpdated,
                                           @OptionalParam(name = Constants.PARAM_PROFILE) UriAndListParam profile,
                                           @OptionalParam(name = Constants.PARAM_SOURCE) UriAndListParam source,
                                           @OptionalParam(name = Constants.PARAM_SECURITY) TokenAndListParam security,
                                           @OptionalParam(name = Constants.PARAM_TAG) TokenAndListParam tag,
                                           @OptionalParam(name = Constants.PARAM_CONTENT) StringAndListParam content,
                                           @OptionalParam(name = Constants.PARAM_TEXT) StringAndListParam text,
                                           @OptionalParam(name = Constants.PARAM_FILTER) StringAndListParam filter,
                                        //    @OptionalParam(name = Patient.SP_ABATEMENT_AGE) QuantityAndListParam abatementAge,
                                        //    @OptionalParam(name = Patient.SP_ABATEMENT_DATE) DateRangeParam abatementDate,
                                        //    @OptionalParam(name = Patient.SP_ABATEMENT_STRING) StringAndListParam abatementString,
                                        //    @OptionalParam(name = Patient.SP_ASSERTER) ReferenceAndListParam asserter,
                                        //    @OptionalParam(name = Patient.SP_BODY_SITE) TokenAndListParam bodySite,
                                        //    @OptionalParam(name = Patient.SP_CATEGORY) TokenAndListParam category,
                                        //    @OptionalParam(name = Patient.SP_CLINICAL_STATUS) TokenAndListParam clinicalStatus,
                                        //    @OptionalParam(name = Patient.SP_CODE) TokenAndListParam code,
                                        //    @OptionalParam(name = Patient.SP_ENCOUNTER) ReferenceAndListParam encounter,
                                        //    @OptionalParam(name = Patient.SP_EVIDENCE) TokenAndListParam evidence,
                                        //    @OptionalParam(name = Patient.SP_EVIDENCE_DETAIL) ReferenceAndListParam evidenceDetail,
                                           @OptionalParam(name = Patient.SP_IDENTIFIER) TokenAndListParam identifier,
                                        //    @OptionalParam(name = Patient.SP_ONSET_AGE) QuantityAndListParam onsetAge,
                                        //    @OptionalParam(name = Patient.SP_ONSET_DATE) DateRangeParam onsetDate,
                                        //    @OptionalParam(name = Patient.SP_ONSET_INFO) StringAndListParam onsetInfo,
                                        //    @OptionalParam(name = Patient.SP_PATIENT) ReferenceAndListParam patient,
                                        //    @OptionalParam(name = Patient.SP_RECORDED_DATE) DateRangeParam recordedDate,
                                        //    @OptionalParam(name = Patient.SP_SEVERITY) TokenAndListParam severity,
                                        //    @OptionalParam(name = Patient.SP_STAGE) TokenAndListParam stage,
                                        //    @OptionalParam(name = Patient.SP_SUBJECT) ReferenceAndListParam subject,
                                        //    @OptionalParam(name = Patient.SP_VERIFICATION_STATUS) TokenAndListParam verificationStatus,
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

        searchParams.add(Patient.SP_IDENTIFIER, identifier);

        searchParams.setLastUpdated(lastUpdated);
        searchParams.setCount(count);
        searchParams.setOffset(offset);
        searchParams.setSort(sort);
        // Call Camel route with the Patient resource
        return producerTemplate.requestBodyAndHeader("direct:SearchRouteProcess", requestDetails,  Exchange.HTTP_METHOD, "GET", Patient.class);
    }

    @Read(version = true)
    public Patient readPatient(@IdParam IdType id, RequestDetails requestDetails,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            // Call Camel route with the Patient resource
            return producerTemplate.requestBodyAndHeader("direct:ReadRouteProcess", requestDetails, Exchange.HTTP_METHOD, "GET", Patient.class);
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
}


