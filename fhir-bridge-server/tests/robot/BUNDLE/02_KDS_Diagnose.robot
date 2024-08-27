# Copyright (c) 2021 Peter Wohlfarth (Appsfactory GmbH), Wladislaw Wagner (Vitasystems GmbH),
# Dave Petzold (Appsfactory GmbH) & Pauline Schulz (Appsfactory GmbH)
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Resource                ${EXECDIR}/robot/_resources/suite_settings.robot

#Test Setup              generic.prepare new request session    Prefer=return=representation
#...															   Authorization=${AUTHORIZATION['Authorization']}

Force Tags              bundle_create    create



*** Variables ***




*** Test Cases ***

001 Create KDS Diagnose FHIR Bundle
    [Documentation]         1. *CREATE* new EHR record\n\n
        ...                 2. *LOAD* KDS_DIAGNOSE_FHIR_BUNDLE.json_\n\n
    	...                 3. *UPDATE* ``Subject - Identifier - value`` with the _UUID:_ ${subject_id} which was created in EHR record\n\n
        ...                 4. *POST* example JSON to observation endpoint\n\n
    	...                 5. *VALIDATE* the response status
    [Tags]             	kds-diagnose-fhir-bundle    valid   not-ready    not-implemented

	ehr.create new ehr    000_ehr_status.json
    kdsdiagnose.create fhir bundle    KDS Diagnose    kds_diagnose_bundle.json
    kdsdiagnose.validate response - 201

    kdsdiagnose.create openehr aql    kds_person
    kdsdiagnose.validate content response_aql - 201