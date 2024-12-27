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
    fhirbridge.validate response - 200
    fhirbridge.create openehr aql    ${openEhrTemplateId}    ${expectedOpenEhrFileName}
    fhirbridge.validate content response_aql_composition - 201    ${expectedOpenEhrFileName}

create fhir bundle
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    POST /Bundle with ehr reference    ${fhir_bundle_name}    ${fhir_bundle_file_name}

POST /Bundle with ehr reference
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    # Set payload for the FHIR bundle
    ${payload}         Load JSON From File    ${DATA_SET_PATH_KDSFHIRBUNDLE}/${fhir_bundle_file_name}
    Log To Console    \n\nFhir bundle API call for ${fhir_bundle_name}

    ${patient_id_list}=  Get Value From Json    ${payload}    $..entry[0].resource.subject.reference
    Run Keyword If    ${patient_id_list}    Set Suite Variable    ${patient_id}    ${patient_id_list}[0]
        ...                ELSE    Run Keyword    Handle Missing Subject Reference    ${payload}

    # POST call to store the FHIR bundle
    ${resp}=    POST    ${BASE_URL}    body=${payload}
    Log To Console    \nResponse of the post call to store the FHIR bundle: ${resp}

Handle Missing Subject Reference
    [Arguments]         ${payload}
    ${patient_id_list}=    Get Value From Json    ${payload}    $..entry[0].resource.subject.identifier.value
    ${patient_id}=    Get From List    ${patient_id_list}    0
    Set Suite Variable    ${patient_id}    Patient/${patient_id}

validate response - 200
    [Documentation]     Validates response of POST to ${BASE_URL} endpoint
    Integer    response status    200
    Log To Console    \nIn Fhir bundle validate response is 200

create openehr aql
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}
    POST /BundleAQL with ehr reference    ${openehr_template}       ${openehr_canonical_file_name}

POST /BundleAQL with ehr reference
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}

    # Connect To Database   ${DB_URL}
    Connect To Database    psycopg2    ${DB_NAME}    ${DB_USER}    ${DB_PASSWORD}    ${DB_HOST}    ${DB_PORT}
    ${result}=   Query    SELECT ehr_id FROM public.fb_patient_ehr WHERE patient_id = '${patient_id}'
    ${result}=   Get From List    ${result}    0
    ${ehr_id}=    Get from list    ${result}    0
    Log To Console    \nehrId fetched from fb_patient_ehr db: ${ehr_id}
    Disconnect From Database

    # Replace get_composition.json file name with the variable.
    ${aql_body}=    Load JSON From File    ${AQL_QUERY}/get_composition.json
    Log To Console    \nOriginal AQL body: ${aql_body}

    # Extract the query string
    ${query_string}=   Get Value From Json    ${aql_body}    $.q
    Log To Console    \nExtracted query string: ${query_string}

    # Replace placeholders with new values
    ${updated_query_string}=    Replace String    ${query_string}[0]    {{ehrUid}}    ${ehr_id}
    ${updated_query_string}=    Replace String    ${updated_query_string}    {{templateId}}    ${openehr_template}
    Log To Console    \nupdated query string: ${updated_query_string}

    # Update the JSON content with the new query string
    ${payload}=      Update Value To Json     ${aql_body}    $.q    ${updated_query_string}
    Log To Console    \nupdated aql body: ${payload}

    # POST CALL TO GET AQL RESPONSE
    ${HEADERS}    Create Dictionary   Content-Type=application/json   Authorization=Basic bXl1c2VyOm15UGFzc3dvcmQ0MzI=   Prefer=return=representation
    &{resp_aql}             POST    ${EHRBASE_URL}/query/aql    body=${payload}    headers=${HEADERS}
    # Log To Console    \nAQL Response: &{resp_aql}
    Set Suite Variable    ${response_aql}    ${resp_aql}


    ${composition}=  Get Value From Json    ${response_aql}    $..rows[0][0]
    ${resp_composition}=    Get From List    ${composition}    0
    # Log To Console    \nAQL Response composition: ${resp_composition}
    ${template_id}=  Get Value From Json    ${resp_composition}    $..archetype_details.template_id.value
    Log To Console    \nResponse Template ID: ${template_id}

    Set Suite Variable    ${response_aql_composition}   ${resp_composition}
    Set Suite Variable    ${response_template_id}   ${template_id}



validate content response_aql_composition - 201
    [Arguments]         ${expected_openehr_file_name}
    Log To Console    \nIn openEHR AQL validate response for ${expected_openehr_file_name}
    # Load the expected JSON content
    ${expected_resp_composition}=    Load JSON From File    ${EHR_COMPOSITION}/${expected_openehr_file_name}
    # Log To Console    \nEXP Response composition:: ${expected_resp_composition}

    ${expected_template_id}=  Get Value From Json    ${expected_resp_composition}    $..archetype_details.template_id.value
    Log To Console    Expected Template ID: ${expected_template_id}\n

    Should Be Equal    ${expected_template_id}    ${response_template_id}
    Log To Console    Both Template ID should be equal:
    Log To Console    Expected Template ID: ${expected_template_id}
    Log To Console    Response Template ID: ${response_template_id}


    # TODO: For compairing whole json value of expected composition and the response composition
    # Normalize both the JSONs by ignoring value of "value" if "_type" == "DV_DATE_TIME"
    # ${normalized_expected_json}=    Normalize Json    ${expected_resp_composition}
    # ${normalized_response_json}=    Normalize Json    ${response_aql_composition}

    # Compare the normalized JSONs returned by the openEHR and the expected cannonical json
    # checking if both json have same sets of key and value pairs other then the ignored fields of comparison .
    # Should Be Equal    ${expected_resp_composition}    ${response_aql_composition}

#Normalize Json
#    [Arguments]    ${json_data}
#    # get normalized json by calling the method normalize_json with ${json_data} in json_normalizer.py file
#    ${normalized_json}=    Evaluate    json_normalizer.normalize_json(${json_data})    modules=json_normalizer
#    RETURN    ${normalized_json}
