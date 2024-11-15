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

package org.ehrbase.fhirbridge.fhir.medicationadministration;

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
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.ResourceType;
import org.openehealth.ipf.commons.ihe.fhir.AbstractPlainProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implementation of {@link org.openehealth.ipf.commons.ihe.fhir.FhirProvider FhirProvider} that provides REST support
 * for 'Find Medication Administration' transaction.
 *
 * @since 1.0.0
 */
@SuppressWarnings({"unused", "java:S107", "DuplicatedCode"})
public class FindMedicationAdministrationProvider extends AbstractPlainProvider {

    @Search(type = MedicationAdministration.class)
    public IBundleProvider searchMedicationAdministration(@OptionalParam(name = IAnyResource.SP_RES_ID) TokenAndListParam id,
                                                          @OptionalParam(name = IAnyResource.SP_RES_LANGUAGE) StringAndListParam language,
                                                          @OptionalParam(name = Constants.PARAM_LASTUPDATED) DateRangeParam lastUpdated,
                                                          @OptionalParam(name = Constants.PARAM_PROFILE) UriAndListParam profile,
                                                          @OptionalParam(name = Constants.PARAM_SOURCE) UriAndListParam resourceSource,
                                                          @OptionalParam(name = Constants.PARAM_SECURITY) TokenAndListParam security,
                                                          @OptionalParam(name = Constants.PARAM_TAG) TokenAndListParam tag,
                                                          @OptionalParam(name = Constants.PARAM_CONTENT) StringAndListParam content,
                                                          @OptionalParam(name = Constants.PARAM_TEXT) StringAndListParam text,
                                                          @OptionalParam(name = Constants.PARAM_FILTER) StringAndListParam filter,
                                                          @OptionalParam(name = MedicationAdministration.SP_CODE) TokenAndListParam code,
                                                          @OptionalParam(name = MedicationAdministration.SP_CONTEXT) ReferenceAndListParam context,
                                                          @OptionalParam(name = MedicationAdministration.SP_DEVICE) ReferenceAndListParam device,
                                                          @OptionalParam(name = MedicationAdministration.SP_EFFECTIVE_TIME) DateRangeParam effectiveTime,
                                                          @OptionalParam(name = MedicationAdministration.SP_IDENTIFIER) TokenAndListParam identifier,
                                                          @OptionalParam(name = MedicationAdministration.SP_MEDICATION) ReferenceAndListParam medication,
                                                          @OptionalParam(name = MedicationAdministration.SP_PATIENT) ReferenceAndListParam patient,
                                                          @OptionalParam(name = MedicationAdministration.SP_PERFORMER) ReferenceAndListParam performer,
                                                          @OptionalParam(name = MedicationAdministration.SP_REASON_GIVEN) TokenAndListParam reasonGiven,
                                                          @OptionalParam(name = MedicationAdministration.SP_REASON_NOT_GIVEN) TokenAndListParam reasonNotGiven,
                                                          @OptionalParam(name = MedicationAdministration.SP_REQUEST) ReferenceAndListParam spRequest,
                                                          @OptionalParam(name = MedicationAdministration.SP_STATUS) TokenAndListParam status,
                                                          @OptionalParam(name = MedicationAdministration.SP_SUBJECT) ReferenceAndListParam subject,
                                                          @Count Integer count, @Offset Integer offset, @Sort SortSpec sort,
                                                          RequestDetails requestDetails, HttpServletRequest request, HttpServletResponse response) {

        SearchParameterMap searchParams = new SearchParameterMap();
        searchParams.add(IAnyResource.SP_RES_ID, id);
        searchParams.add(IAnyResource.SP_RES_LANGUAGE, language);

        searchParams.add(Constants.PARAM_PROFILE, profile);
        searchParams.add(Constants.PARAM_SOURCE, resourceSource);
        searchParams.add(Constants.PARAM_SECURITY, security);
        searchParams.add(Constants.PARAM_TAG, tag);
        searchParams.add(Constants.PARAM_CONTENT, content);
        searchParams.add(Constants.PARAM_TEXT, text);
        searchParams.add(Constants.PARAM_FILTER, filter);

        searchParams.add(MedicationAdministration.SP_CODE, code);
        searchParams.add(MedicationAdministration.SP_CONTEXT, context);
        searchParams.add(MedicationAdministration.SP_DEVICE, device);
        searchParams.add(MedicationAdministration.SP_EFFECTIVE_TIME, effectiveTime);
        searchParams.add(MedicationAdministration.SP_IDENTIFIER, identifier);
        searchParams.add(MedicationAdministration.SP_MEDICATION, medication);
        searchParams.add(MedicationAdministration.SP_PATIENT, patient);
        searchParams.add(MedicationAdministration.SP_PERFORMER, performer);
        searchParams.add(MedicationAdministration.SP_REASON_GIVEN, reasonGiven);
        searchParams.add(MedicationAdministration.SP_REASON_NOT_GIVEN, reasonNotGiven);
        searchParams.add(MedicationAdministration.SP_REQUEST, spRequest);
        searchParams.add(MedicationAdministration.SP_STATUS, status);
        searchParams.add(MedicationAdministration.SP_SUBJECT, subject);

        searchParams.setLastUpdated(lastUpdated);
        searchParams.setCount(count);
        searchParams.setOffset(offset);
        searchParams.setSort(sort);

        return requestBundleProvider(searchParams, null, ResourceType.MedicationAdministration.name(), request, response, requestDetails);
    }

    @Read(version = true)
    public MedicationAdministration readMedicationAdministration(@IdParam IdType id, RequestDetails requestDetails,
                                                                 HttpServletRequest request, HttpServletResponse response) {
        return requestResource(id, null, MedicationAdministration.class, request, response, requestDetails);
    }
}
