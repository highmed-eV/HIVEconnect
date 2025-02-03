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

package org.ehrbase.fhirbridge.fhir.bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BundleResourceProvider implements IResourceProvider  {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public Class<Bundle> getResourceType() {
        return Bundle.class;
    }
    
    @Create
    public MethodOutcome create(@ResourceParam Bundle bundle,
                                RequestDetails requestDetails,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        System.out.println("Executing 'Provide Bundle' transaction using 'create' operation...");
    
        FhirContext fhirContext = FhirContext.forR4();
        String inputResource = fhirContext.newJsonParser().encodeResourceToString(bundle);
   
        // Call Camel route with the Bundle resource
        String processedBundle = producerTemplate.requestBody("direct:CamelCreateRouteProcess", inputResource, String.class);

        // System.out.println("processedBundle..." + processedBundle);
        Bundle processedBundleResource = fhirContext.newJsonParser().parseResource(Bundle.class, processedBundle);

        // Return the MethodOutcome
        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setCreated(true);
        
        // Store all created resource IDs
        List<String> createdResourceIds = new ArrayList<>();
        OperationOutcome finalOutcome = new OperationOutcome();

        // Iterate over all entries in the Bundle
        for (Bundle.BundleEntryComponent entry : processedBundleResource.getEntry()) {
            if (entry.hasResponse() && entry.getResponse().hasLocation()) {
                String resourceId = entry.getResponse().getLocation();
                createdResourceIds.add(resourceId); // Collect all created resource IDs
            }

            // If there is an OperationOutcome, append its issues to the final outcome
            if (entry.hasResponse() && entry.getResponse().hasOutcome()) {
                if (entry.getResponse().getOutcome() instanceof OperationOutcome) {
                    OperationOutcome outcome = (OperationOutcome) entry.getResponse().getOutcome();
                    finalOutcome.getIssue().addAll(outcome.getIssue());
                }
            }
        }

        // Set the last created resource ID as the main ID in MethodOutcome
        if (!createdResourceIds.isEmpty()) {
            methodOutcome.setId(new IdType(createdResourceIds.get(createdResourceIds.size() - 1)));
        }

        // Attach the combined OperationOutcome
        if (!finalOutcome.getIssue().isEmpty()) {
            methodOutcome.setOperationOutcome(finalOutcome);
        }

        return methodOutcome;
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
                                        //    @OptionalParam(name = Bundle.SP_ABATEMENT_AGE) QuantityAndListParam abatementAge,
                                        //    @OptionalParam(name = Bundle.SP_ABATEMENT_DATE) DateRangeParam abatementDate,
                                        //    @OptionalParam(name = Bundle.SP_ABATEMENT_STRING) StringAndListParam abatementString,
                                        //    @OptionalParam(name = Bundle.SP_ASSERTER) ReferenceAndListParam asserter,
                                        //    @OptionalParam(name = Bundle.SP_BODY_SITE) TokenAndListParam bodySite,
                                        //    @OptionalParam(name = Bundle.SP_CATEGORY) TokenAndListParam category,
                                        //    @OptionalParam(name = Bundle.SP_CLINICAL_STATUS) TokenAndListParam clinicalStatus,
                                        //    @OptionalParam(name = Bundle.SP_CODE) TokenAndListParam code,
                                        //    @OptionalParam(name = Bundle.SP_ENCOUNTER) ReferenceAndListParam encounter,
                                        //    @OptionalParam(name = Bundle.SP_EVIDENCE) TokenAndListParam evidence,
                                        //    @OptionalParam(name = Bundle.SP_EVIDENCE_DETAIL) ReferenceAndListParam evidenceDetail,
                                           @OptionalParam(name = Bundle.SP_IDENTIFIER) TokenAndListParam identifier,
                                        //    @OptionalParam(name = Bundle.SP_ONSET_AGE) QuantityAndListParam onsetAge,
                                        //    @OptionalParam(name = Bundle.SP_ONSET_DATE) DateRangeParam onsetDate,
                                        //    @OptionalParam(name = Bundle.SP_ONSET_INFO) StringAndListParam onsetInfo,
                                        //    @OptionalParam(name = Bundle.SP_PATIENT) ReferenceAndListParam patient,
                                        //    @OptionalParam(name = Bundle.SP_RECORDED_DATE) DateRangeParam recordedDate,
                                        //    @OptionalParam(name = Bundle.SP_SEVERITY) TokenAndListParam severity,
                                        //    @OptionalParam(name = Bundle.SP_STAGE) TokenAndListParam stage,
                                        //    @OptionalParam(name = Bundle.SP_SUBJECT) ReferenceAndListParam subject,
                                        //    @OptionalParam(name = Bundle.SP_VERIFICATION_STATUS) TokenAndListParam verificationStatus,
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

        // searchParams.add(Bundle.SP_ABATEMENT_AGE, abatementAge);
        // searchParams.add(Bundle.SP_ABATEMENT_DATE, abatementDate);
        // searchParams.add(Bundle.SP_ABATEMENT_STRING, abatementString);
        // searchParams.add(Bundle.SP_ASSERTER, asserter);
        // searchParams.add(Bundle.SP_BODY_SITE, bodySite);
        // searchParams.add(Bundle.SP_CATEGORY, category);
        // searchParams.add(Bundle.SP_CLINICAL_STATUS, clinicalStatus);
        // searchParams.add(Bundle.SP_CODE, code);
        // searchParams.add(Bundle.SP_ENCOUNTER, encounter);
        // searchParams.add(Bundle.SP_EVIDENCE, evidence);
        // searchParams.add(Bundle.SP_EVIDENCE_DETAIL, evidenceDetail);
        searchParams.add(Bundle.SP_IDENTIFIER, identifier);
        // searchParams.add(Bundle.SP_ONSET_AGE, onsetAge);
        // searchParams.add(Bundle.SP_ONSET_DATE, onsetDate);
        // searchParams.add(Bundle.SP_ONSET_INFO, onsetInfo);
        // searchParams.add(Bundle.SP_PATIENT, patient);
        // searchParams.add(Bundle.SP_RECORDED_DATE, recordedDate);
        // searchParams.add(Bundle.SP_SEVERITY, severity);
        // searchParams.add(Bundle.SP_STAGE, stage);
        // searchParams.add(Bundle.SP_SUBJECT, subject);
        // searchParams.add(Bundle.SP_VERIFICATION_STATUS, verificationStatus);

        searchParams.setLastUpdated(lastUpdated);
        searchParams.setCount(count);
        searchParams.setOffset(offset);
        searchParams.setSort(sort);
        // Call Camel route with the Bundle resource
        Bundle processedBundle = producerTemplate.requestBody("direct:CamelSearchRouteProcess", requestDetails, Bundle.class);

        return processedBundle;
    }

    @Read(version = true)
    public Bundle readBundle(@IdParam IdType id, RequestDetails requestDetails,
                                   HttpServletRequest request, HttpServletResponse response) {
        // Call Camel route with the Bundle resource
        Bundle processedBundle = producerTemplate.requestBody("direct:CamelCreateRouteProcessRoute", requestDetails, Bundle.class);

        return processedBundle;
    }    
}


