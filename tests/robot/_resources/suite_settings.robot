# Copyright (c) 2020 Wladislaw Wagner (Vitasystems GmbH)
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

Library     REST    ssl_verify=false
Library     String
Library     Collections
Library     OperatingSystem
Library     Process
Library     JSONLibrary
Library     BuiltIn
Library     DatabaseLibrary
Library     ${EXECDIR}/robot/_resources/functions/json_normalizer.py
Library     ${EXECDIR}/robot/_resources/functions/load_json_utf8.py

Resource    ${EXECDIR}/robot/_resources/keywords/ehr.robot
Resource    ${EXECDIR}/robot/_resources/keywords/hiveconnect.robot

Variables   ${EXECDIR}/robot/_resources/variables/sut_config.py
            ...    ${SUT}



*** Variables ***

${BASE_URL}                             http://localhost:8888/hive-connect/fhir/Bundle
${USERNAME}                             hiveconnect-user
${PASSWORD}                             myPassword1234
${AUTH}                                 Basic aGl2ZWNvbm5lY3QtdXNlcjpteVBhc3N3b3JkMTIzNA==
${INPUT_PATIENT_ID}                     1
${EHRBASE_URL}                          http://localhost:8080/ehrbase/rest/openehr/v1
${EHRBASE_USER}                         ehrbase
${EHRBASE_PASS}                         ehrbase
${TEST_CASE_LIST_FILE}                  ${EXECDIR}/robot/BUNDLE/test_case_list.json
${EHR_COMPOSITION}                      ${EXECDIR}/robot/_resources/test_data/outputOpenEhr
${DATA_SET_PATH_KDSFHIRBUNDLE}          ${EXECDIR}/robot/_resources/test_data/inputFhirBundles
${VALID EHR DATA SETS}                  ${EXECDIR}/robot/_resources/test_data/ehr/valid
${AQL_QUERY}                            ${EXECDIR}/robot/_resources/test_data/aql
${OUTPUT_LEVEL}                         verbose

${SUT}                                  LOCAL
${AUTH_TYPE}                            BASIC
${NODOCKER}                             False
${CODE_COVERAGE}                        False
${REDUMP_REQUIRED}                      ${FALSE}
${ALLOW-TEMPLATE-OVERWRITE}             ${TRUE}
${CACHE-ENABLED}                        ${TRUE}

${DB_HOST}    localhost
${DB_PORT}    5432
${DB_NAME}    hiveconnect
${DB_USER}    postgres
${DB_PASSWORD}    postgres
${DB_DRIVER}      org.postgresql.Driver
${DB_URL}         jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
