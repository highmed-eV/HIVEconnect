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

package org.ehrbase.fhirbridge.fhir.servicerequest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openehealth.ipf.commons.ihe.fhir.AbstractPlainProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implementation of {@link org.openehealth.ipf.commons.ihe.fhir.FhirProvider FhirProvider} that provides REST support
 * for 'Find Service Request' transaction.
 *
 * @since 1.0.0
 */
@SuppressWarnings({"unused", "java:S107", "DuplicatedCode"})
public class FindServiceRequestProvider extends AbstractPlainProvider {

    @Search(type = ServiceRequest.class)
    public IBundleProvider searchServiceRequest(@OptionalParam(name = IAnyResource.SP_RES_ID) TokenAndListParam id,
                                                 @OptionalParam(name = IAnyResource.SP_RES_LANGUAGE) StringAndListParam language,
                                                 @OptionalParam(name = Constants.PARAM_LASTUPDATED) DateRangeParam lastUpdated,
                                                 @OptionalParam(name = Constants.PARAM_PROFILE) UriAndListParam profile,
                                                 @OptionalParam(name = Constants.PARAM_SOURCE) UriAndListParam source,
                                                 @OptionalParam(name = Constants.PARAM_SECURITY) TokenAndListParam security,
                                                 @OptionalParam(name = Constants.PARAM_TAG) TokenAndListParam tag,
                                                 @OptionalParam(name = Constants.PARAM_CONTENT) StringAndListParam content,
                                                 @OptionalParam(name = Constants.PARAM_TEXT) StringAndListParam text,
                                                 @OptionalParam(name = Constants.PARAM_FILTER) StringAndListParam filter,
                                                 @OptionalParam(name = ServiceRequest.SP_AUTHORED) DateRangeParam authored,
                                                 @OptionalParam(name = ServiceRequest.SP_BASED_ON) ReferenceAndListParam basedOn,
                                                 @OptionalParam(name = ServiceRequest.SP_BODY_SITE) TokenAndListParam bodySite,
                                                 @OptionalParam(name = ServiceRequest.SP_CATEGORY) TokenAndListParam category,
                                                 @OptionalParam(name = ServiceRequest.SP_CODE) TokenAndListParam code,
                                                 @OptionalParam(name = ServiceRequest.SP_ENCOUNTER) ReferenceAndListParam encounter,
                                                 @OptionalParam(name = ServiceRequest.SP_IDENTIFIER) TokenAndListParam identifier,
                                                 @OptionalParam(name = ServiceRequest.SP_INSTANTIATES_CANONICAL) ReferenceAndListParam instantiatesCanonical,
                                                 @OptionalParam(name = ServiceRequest.SP_INSTANTIATES_URI) UriAndListParam instantiatesUri,
                                                 @OptionalParam(name = ServiceRequest.SP_INTENT) TokenAndListParam intent,
                                                 @OptionalParam(name = ServiceRequest.SP_PATIENT) ReferenceAndListParam patient,
                                                 @OptionalParam(name = ServiceRequest.SP_PERFORMER) ReferenceAndListParam performer,
                                                 @OptionalParam(name = ServiceRequest.SP_PERFORMER_TYPE) TokenAndListParam performerType,
                                                 @OptionalParam(name = ServiceRequest.SP_PRIORITY) TokenAndListParam priority,
                                                 @OptionalParam(name = ServiceRequest.SP_REPLACES) ReferenceAndListParam replaces,
                                                 @OptionalParam(name = ServiceRequest.SP_REQUESTER) ReferenceAndListParam requester,
                                                 @OptionalParam(name = ServiceRequest.SP_REQUISITION) TokenAndListParam requisition,
                                                 @OptionalParam(name = ServiceRequest.SP_SPECIMEN) ReferenceAndListParam specimen,
                                                 @OptionalParam(name = ServiceRequest.SP_STATUS) TokenAndListParam status,
                                                 @OptionalParam(name = ServiceRequest.SP_SUBJECT) ReferenceAndListParam subject,
                                                 @OptionalParam(name = ServiceRequest.SP_OCCURRENCE) DateRangeParam occurrence,
                                                 @Count Integer count, @Offset Integer offset, @Sort SortSpec sort,
                                                 RequestDetails requestDetails, HttpServletRequest request, HttpServletResponse response) {

        SearchParameterMap searchParams = new SearchParameterMap();
        searchParams.add(IAnyResource.SP_RES_ID, id);
        searchParams.add(IAnyResource.SP_RES_LANGUAGE, language);

        searchParams.add(Constants.PARAM_PROFILE, profile);
        searchParams.add(Constants.PARAM_SOURCE, source);
        searchParams.add(Constants.PARAM_SECURITY, security);
        searchParams.add(Constants.PARAM_TAG, tag);
        searchParams.add(Constants.PARAM_CONTENT, content);
        searchParams.add(Constants.PARAM_TEXT, text);
        searchParams.add(Constants.PARAM_FILTER, filter);

        searchParams.add(ServiceRequest.SP_AUTHORED, authored);
        searchParams.add(ServiceRequest.SP_BASED_ON, basedOn);
        searchParams.add(ServiceRequest.SP_BODY_SITE, bodySite);
        searchParams.add(ServiceRequest.SP_CATEGORY, category);
        searchParams.add(ServiceRequest.SP_CODE, code);
        searchParams.add(ServiceRequest.SP_ENCOUNTER, encounter);
        searchParams.add(ServiceRequest.SP_IDENTIFIER, identifier);
        searchParams.add(ServiceRequest.SP_INSTANTIATES_CANONICAL, instantiatesCanonical);
        searchParams.add(ServiceRequest.SP_INSTANTIATES_URI, instantiatesUri);
        searchParams.add(ServiceRequest.SP_INTENT, intent);
        searchParams.add(ServiceRequest.SP_PATIENT, patient);
        searchParams.add(ServiceRequest.SP_PERFORMER, performer);
        searchParams.add(ServiceRequest.SP_PERFORMER_TYPE, performerType);
        searchParams.add(ServiceRequest.SP_PRIORITY, priority);
        searchParams.add(ServiceRequest.SP_REPLACES, replaces);
        searchParams.add(ServiceRequest.SP_REQUESTER, requester);
        searchParams.add(ServiceRequest.SP_REQUISITION, requisition);
        searchParams.add(ServiceRequest.SP_SPECIMEN, specimen);
        searchParams.add(ServiceRequest.SP_STATUS, status);
        searchParams.add(ServiceRequest.SP_SUBJECT, subject);
        searchParams.add(ServiceRequest.SP_OCCURRENCE, occurrence);

        searchParams.setLastUpdated(lastUpdated);
        searchParams.setCount(count);
        searchParams.setOffset(offset);
        searchParams.setSort(sort);

        return requestBundleProvider(searchParams, null, ResourceType.ServiceRequest.name(), request, response, requestDetails);
    }

    @Read(version = true)
    public ServiceRequest readServiceRequest(@IdParam IdType id, RequestDetails requestDetails,
                                             HttpServletRequest request, HttpServletResponse response) {
        return requestResource(id, null, ServiceRequest.class, request, response, requestDetails);
    }
}
