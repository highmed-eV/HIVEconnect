*** Settings ***
Library    JSONLibrary
*** Keywords ***
load test cases from json
    # load json from test_case_list.json file
    ${json_data}=    Load JSON From File    ${TEST_CASE_LIST_FILE}
    # get the json dictionary for all the tes cases test cases
    ${TEST_CASES}=    Get From Dictionary    ${json_data}    testcases
    Set Suite Variable    ${TEST_CASES}

create and validate fhir bundle
    [Documentation]         1. *CREATE* new EHR record\n\n
            ...             2. *CREATE* FHIR bundle json and post it to FHIR server\n\n
            ...             3. *VALIDATE* the FHIR response status\n\n
            ...             4. *CREATE* openEHR AQL and post it to openEHR server\n\n
            ...             5. *VALIDATE* the content of the AQL response.
    [Arguments]    ${testCaseName}    ${inputBundleFileName}    ${expectedOpenEhrFileName}    ${openEhrTemplateId}
    ehr.create new ehr    000_ehr_status.json
    fhirbridge.create fhir bundle    ${testCaseName}    ${inputBundleFileName}
    fhirbridge.validate response - 201      ${expectedOpenEhrFileName}
    fhirbridge.create openehr aql    ${openEhrTemplateId}    ${expectedOpenEhrFileName}
    fhirbridge.validate content response_aql - 201    ${expectedOpenEhrFileName}

create fhir bundle
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    POST /Bundle with ehr reference    ${fhir_bundle_name}    ${fhir_bundle_file_name}

POST /Bundle with ehr reference
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    # set payload for the fhir bundle
    ${payload}         Load JSON From File    ${DATA_SET_PATH_KDSFHIRBUNDLE}/${fhir_bundle_file_name}
    Log To Console    Fhir bundle API call for ${fhir_bundle_name}
    # POST CALL TO STORE THE FHIR BUNDLE
    &{resp}             POST    ${BASE_URL}    body=${payload}
    Log To Console    Response of the post call to store the fhir bundle &{resp}

validate response - 201
    [Documentation]     Validates response of POST to ${BASE_URL} endpoint
    Integer    response status    201
    [Arguments]         ${expected_openehr_canonical_file_name}
    # compare expected_openehr_canonical_file_name with output(response_aql)
    Log To Console    In Fhir bundle validate response

create openehr aql
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}
    POST /BundleAQL with ehr reference    ${openehr_template}       ${openehr_canonical_file_name}

POST /BundleAQL with ehr reference
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}

    #    Replace get_composition.json file name with the variable.
    ${aql_body}=    Load JSON From File    ${EHR_COMPOSITION}/../aql/get_composition.json

    # Extract the query string
    ${query_string}=   Get Value From Json    ${aql_body}    $.q

    # Replace placeholders with new values
    ${updated_query_string}=    Replace String    ${query_string}[0]    {{ehrUid}}    ${ehr_id}

    ${updated_query_string}=    Replace String    ${updated_query_string}    {{templateId}}    ${openehr_template}

    # Update the JSON content with the new query string
    ${aql_body}=      Update Value To Json     ${aql_body}    $.q    ${updated_query_string}

    # replace the template id - ${openehr_template} in the aql and pass to openehr cdr

    # POST CALL TO GET AQL RESPONSE
    &{resp_ehr}             POST    ${EHRBASE_URL}/query/aql    body=${payload}
    Log To Console    AQL Response &{resp_ehr}
    # currently mocking the aql response from the file
#    &{resp_ehr}=    Load JSON From File    ${EHR_COMPOSITION}/${openehr_canonical_file_name}
    Log To Console    In openEHR AQL API call for ${openehr_canonical_file_name}
                    Set Suite Variable    ${response_aql}    ${resp_ehr}

validate content response_aql - 201
    [Arguments]         ${expected_openehr_file_name}
    Log To Console    In openEHR AQL validate response for ${expected_openehr_file_name}\n
    # Load the expected JSON content
    ${expected_resp_ehr}=    Load JSON From File    ${EHR_COMPOSITION}/${expected_openehr_file_name}

    # Normalize both the JSONs by ignoring value of "value" if "_type" == "DV_DATE_TIME"
    ${normalized_expected_json}=    Normalize Json    ${expected_resp_ehr}
    ${normalized_response_json}=    Normalize Json    ${response_aql}

    # Compare the normalized JSONs returned by the openEHR and the expected cannonical json
    # checking if both json have same sets of key and value pairs other then the ignored fields of comparison .
    Should Be Equal    ${normalized_expected_json}    ${normalized_response_json}

Normalize Json
    [Arguments]    ${json_data}
    # get normalized json by calling the method normalize_json with ${json_data} in json_normalizer.py file
    ${normalized_json}=    Evaluate    json_normalizer.normalize_json(${json_data})    modules=json_normalizer
    RETURN    ${normalized_json}







