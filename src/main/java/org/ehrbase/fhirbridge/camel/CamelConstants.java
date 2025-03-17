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

package org.ehrbase.fhirbridge.camel;

/**
 * Constants used by the FHIR Bridge.
 *
 * @since 1.0.0
 */
public final class CamelConstants {

    public static final String TEMP_REQUEST_RESOURCE_STRING = "CamelTempRequestResourceString";

    // public static final String TEMP_REQUEST_RESOURCE_OBJECT = "CamelTempRequestResourceObject";

     public static final String OPENEHR_COMPOSITION_ID = "CamelOpenEhrCompositionId";
     
     public static final String REQUEST_HTTP_METHOD = "CamelRequestHttpMethod";

     public static final String REQUESTDETAILS_ID = "CamelRequestDetailsId";
    
     public static final String REQUESTDETAILS_OPERATION_TYPE = "CamelRequestDetailsOperationType";

     public static final String REQUEST_RESOURCE_TYPE = "CamelRequestResourceResourceType";
    
     public static final String REQUEST_RESOURCE = "CamelRequestResource";
    
     public static final String REQUEST_REMOTE_SYSTEM_ID = "CamelRequestRemoteSystemId";
    
     public static final String FHIR_INPUT_PROFILE = "CamelFhirInputProfile";
     
     public static final String FHIR_INPUT_SOURCE = "CamelFhirInputSource";
    
    public static final String FHIR_SERVER_RESOURCE_ID = "CamelFhirServerResourceId";

    public static final String FHIR_INPUT_PATIENT_ID = "CamelFhirPatientId";

    public static final String FHIR_INPUT_PATIENT_SEARCH_URL = "CamelFhirPatientSearchUrl";

    public static final String FHIR_INPUT_PATIENT_ID_TYPE = "CamelFhirInputPatientIdType";

    public static final String FHIR_SERVER_PATIENT_ID = "CamelFhirServerPatientId";

    public static final String FHIR_SERVER_PATIENT_RESOURCE = "CamelFhirServerPatientResource";

    public static final String FHIR_REQUEST_RESOURCE_IDS = "CamelFhirRequestResourceIds";

    public static final String FHIR_REFERENCE_REQUEST_RESOURCE_IDS = "CamelFhirReferenceRequestResourceIds";

    public static final String FHIR_REFERENCE_INTERNAL_RESOURCE_IDS = "CamelFhirReferenceInternalResourceIds";

    public static final String FHIR_REFERENCE_INTERNAL_RESOURCE_IDS_TEMP = "CamelFhirReferenceInternalResourceIdsTemp";

    public static final String FHIR_SERVER_EXISTING_RESOURCES = "CamelFhirServerExistingResources";

    public static final String FHIR_SERVER_OUTCOME = "CamelFhirServerOutcome";

    public static final String FHIR_INTERNAL_RESOURCE_TYPE = "CamelFhirInternalResourceType";

    public static final String FHIR_INTERAL_EXISTING_ID = "CamelFhirInternalExistingId";

    
    public static final String OPEN_FHIR_SERVER_OUTCOME = "CamelOpenFhirServerOutcome";

    public static final String OPEN_EHR_SERVER_OUTCOME = "CamelOpenEHRServerOutcome";

    public static final String OPEN_EHR_SERVER_OUTCOME_COMPOSITION = "CamelOpenEHRServerOutcomeComposition";

    public static final String OUTCOME = "CamelFhirBridgeOutcome";

    public static final String IDENTIFIER_OBJECT = "CamelIdentifierObject";

    public static final String IDENTIFIER_STRING = "CamelIdentifierString";

    
    private CamelConstants() {
    }
}
