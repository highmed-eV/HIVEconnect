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

    public static final String MINIO_OBJECT = "CamelMinioObject";

    public static final String COMPOSITION_ID = "CamelFhirBridgeCompositionId";

    public static final String INPUT_RESOURCE = "CamelFhirBridgeIncomingResource";
    
    public static final String INPUT_RESOURCE_TYPE = "CamelFhirBridgeIncomingResourceType";
    
    public static final String FHIR_SERVER_OUTCOME = "CamelFhirServerOutcome";

    public static final String OPEN_FHIR_SERVER_OUTCOME = "CamelOpenFhirServerOutcome";

    public static final String OPEN_EHR_SERVER_OUTCOME = "CamelOpenEHRServerOutcome";

    public static final String OUTCOME = "CamelFhirBridgeOutcome";

    public static final String SERVER_PATIENT_RESOURCE = "CamelFhirServerPatientResource";

    public static final String PATIENT_ID = "CamelFhirPatientId";

    public static final String SERVER_PATIENT_ID = "CamelFhirServerPatientId";

    public static final String PROFILE = "CamelFhirBridgeProfile";

    public static final String RESOURCE_ID = "FhirBridgeResourceId";

    public static final String INPUT_RESOURCE_IDS = "FhirBridgeResourceIds";

    public static final String INTERNAL_RESOURCE_IDS = "FhirServerResourceIds";

    public static final String SERVER_EXISTING_RESOURCES = "CamelFhirServerExistingResources";

    public static final String INTERNAL_RESOURCE_TYPE = "FhirServerResourceType";

    public static final String STRING_INTERAL_ID = "FhirServerExistingId";

    private CamelConstants() {
    }
}
