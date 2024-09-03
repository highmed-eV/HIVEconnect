*** Keywords ***
create fhir bundle
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    POST /Bundle with ehr reference    ${fhir_bundle_name}    ${fhir_bundle_file_name}

POST /Bundle with ehr reference
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    ${payload}         Load JSON From File    ${DATA_SET_PATH_KDSFHIRBUNDLE}/${fhir_bundle_file_name}
    Log To Console    Fhir bundle API call for ${fhir_bundle_name}
#    POST CALL TO STORE THE FHIR BUNDLE
#    &{resp}             POST    ${BASE_URL}/Bundle    body=${payload}
#    Output Debug Info To Console

validate response - 201
#    [Documentation]     Validates response of POST to ${BASE_URL}/BUNDLE_NAME endpoint
#                        Integer    response status    201
    [Arguments]         ${expected_openehr_canonical_file_name}
    #compare expected_openehr_canonical_file_name with output(response_aql)
    Log To Console    In Fhir bundle validate response

create openehr aql
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}
    POST /BundleAQL with ehr reference    ${openehr_template}       ${openehr_canonical_file_name}

POST /BundleAQL with ehr reference
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}
    #    replace the template id in the aql and pass to openehr cdr
    #    POST CALL TO GET AQL RESPONSE
    #    &{resp_ehr}             POST    ${BASE_URL}/ehrbase/rest/openehr/v1/query/aql    body=${payload}
    #                    Output Debug Info To Console
    #    currently mocking the aql response from the file
    &{resp_ehr}=    Load JSON From File    ${EHR_COMPOSITION}/${openehr_canonical_file_name}
    Log To Console    In openEHR AQL API call for ${openehr_canonical_file_name}
                    Set Suite Variable    ${response_aql}    ${resp_ehr}

validate content response_aql - 201
    [Arguments]         ${expected_openehr_file_name}
    Log To Console    In openEHR AQL validate response for ${expected_openehr_file_name}\n
    # Step 1: Load the expected JSON content
    ${expected_resp_ehr}=    Load JSON From File    ${EHR_COMPOSITION}/${expected_openehr_file_name}

    # Step 3: Normalize both the JSONs by ignoring value of "value" if "_type" == "DV_DATE_TIME"
    ${normalized_expected_json}=    Normalize Json    ${expected_resp_ehr}
    ${normalized_response_json}=    Normalize Json    ${response_aql}

    # Step 4: Compare the normalized JSONs
    Should Be Equal    ${normalized_expected_json}    ${normalized_response_json}

Normalize Json
    [Arguments]    ${json_data}
    ${normalized_json}=    Evaluate    json_normalizer.normalize_json(${json_data})    modules=json_normalizer
    RETURN    ${normalized_json}

load test cases from json
    ${json_data}=    Load JSON From File    ${TEST_CASE_LIST_FILE}
    ${TEST_CASES}=    Get From Dictionary    ${json_data}    testcases
    Set Suite Variable    ${TEST_CASES}

create and validate fhir bundle
    [Arguments]    ${testCaseName}    ${inputBundleFileName}    ${expectedOpenEhrFileName}    ${openEhrTemplateId}
    ehr.create new ehr    000_ehr_status.json
    fhirbridge.create fhir bundle    ${testCaseName}    ${inputBundleFileName}
    fhirbridge.validate response - 201      ${expectedOpenEhrFileName}
    fhirbridge.create openehr aql    ${openEhrTemplateId}    ${expectedOpenEhrFileName}
    fhirbridge.validate content response_aql - 201    ${expectedOpenEhrFileName}







