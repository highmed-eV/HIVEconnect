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
Force Tags              bundle_create    create

*** Test Cases ***

001 Create FHIR Bundles
    [Documentation]         1. *LOAD* all the test cases from test_case_list.json file\n\n
        ...                 2. *RUN* all the test cases for each bundle in a loop.\n\n
        ...                 3. *CREATE* new EHR record\n\n
        ...                 4. *CREATE* FHIR bundle json and post it to FHIR server\n\n
        ...                 5. *VALIDATE* the FHIR response status\n\n
        ...                 6. *CREATE* openEHR AQL and post it to openEHR server\n\n
        ...                 7. *VALIDATE* the content of the AQL response.
    [Tags]             	    fhir-bundle    valid   not-ready    not-implemented
    ehr.create new ehr    000_ehr_status.json
    # load all the testcases from the test_case_list.json file
    [Setup]    hiveconnect.load test cases from json
    # loop to run the robot test case for each test cases
     FOR   ${testcase}    IN    @{TEST_CASES}
           # set values for each key variable in the each test case
           ${testCaseName}=    Get From Dictionary    ${testcase}    testCaseName
           ${inputBundleFileName}=    Get From Dictionary    ${testcase}    inputBundleFileName
           ${expectedOpenEhrFileName}=    Get From Dictionary    ${testcase}    expectedOpenEhrFileName
           ${openEhrTemplateId}=    Get From Dictionary    ${testcase}    openEhrTemplateId
           # create and validate for each of the test cases
           hiveconnect.create and validate fhir bundle    ${testCaseName}    ${inputBundleFileName}    ${expectedOpenEhrFileName}    ${openEhrTemplateId}
     END
     [Teardown]    Log    All FHIR Bundles and OpenEHR AQLs validated.