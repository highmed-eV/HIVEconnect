*** Keywords ***
load test cases from json
    # Load JSON from the test_case_list.json file using UTF-8 encoding
    ${json_data}=    Load Json Utf8    ${TEST_CASE_LIST_FILE}   ${INPUT_PATIENT_ID}
    #Log To Console    \Input Data composition: ${json_data}

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

    #Set Suite Variable    ${new_ehr_id}    4129d326-bb05-4c04-9ab5-82f734a9a2b7
    #Set Suite Variable    ${subject_id}    resp_subject_id

    hiveconnect.create fhir bundle    ${testCaseName}    ${inputBundleFileName}
    hiveconnect.validate response - 201
    hiveconnect.create openehr aql    ${openEhrTemplateId}    ${expectedOpenEhrFileName}
    hiveconnect.validate content response_aql_composition - 201    ${expectedOpenEhrFileName}

create fhir bundle
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    POST /Bundle with ehr reference    ${fhir_bundle_name}    ${fhir_bundle_file_name}

POST /Bundle with ehr reference
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
    # Set payload for the FHIR bundle
    ${payload_from_file}         Load JSON From File    ${DATA_SET_PATH_KDSFHIRBUNDLE}/${fhir_bundle_file_name}    encoding=UTF-8
    ${payload}=    Update Input Json    ${payload_from_file}   ${INPUT_PATIENT_ID}
    # Log To Console     \n\nFhir bundle ${payload}
    Log To Console     \n\nFhir bundle API call for ${fhir_bundle_name}

#    ${patient_id_list}=  Get Value From Json    ${payload}    $..entry[0].resource.subject.reference
#    Run Keyword If    ${patient_id_list}    Set Suite Variable    ${patient_id}    ${patient_id_list}[0]
#        ...                ELSE    Run Keyword    Handle Subject Identifier    ${payload}

    # POST call to store the FHIR bundle
    ${headers}=    Create Dictionary    Content-Type=application/json   Authorization=${AUTH}
    ${resp}=    POST    ${BASE_URL}    body=${payload}  headers=${headers} 
    Log To Console    \nResponse of the post call to store the FHIR bundle: ${resp}

    # Extract patientId from the bundle.
    # Extract subject reference
    ${subject_ref_list}=    Get Value From Json    ${payload}    $..entry[*].resource.subject.reference
    Run Keyword If          ${subject_ref_list}    Set Suite Variable    ${patient_id}    ${subject_ref_list}[0]
        ...                 ELSE    Run Keyword    Handle Subject Identifier    ${payload}
    Log To Console    \nPatientId normal reference: ${patient_id}

    # Check if subject reference starts with 'urn:uuid'
    Run Keyword If         '${patient_id}'.startswith('urn:uuid')    Handle Subject Reference    ${payload}    ${patient_id}
    Log To Console    \nPatientId normal reference: ${patient_id}

Handle Subject Identifier
    [Arguments]         ${payload}
    ${patient_id_list}=     Get Value From Json    ${payload}    $..entry[*].resource.subject.identifier.value
    ${patient_id}=          Get From List    ${patient_id_list}    0
    Set Suite Variable      ${patient_id}    Patient/${patient_id}

Handle Subject Reference
    [Arguments]         ${payload}    ${subject_ref}
    Log                 Processing subject reference: ${subject_ref}

    # Extract matching patient entry resource
    ${entries}=          Get Value From Json    ${payload}    $.entry[*]
    ${matching_entry}=   Find Matching Entry    ${subject_ref}    ${entries}
    ${resource}=         Get From Dictionary    ${matching_entry}    resource
    Log To Console        Resource: ${resource}

    # Extract patient identifier system and value
    ${identifier_list}=    Get From Dictionary    ${resource}    identifier
    ${first_identifier}=    Get From List    ${identifier_list}    0
    ${PATIENT_PREFIX}    Get From Dictionary    ${first_identifier}    system
    ${resource_id}=      Get From Dictionary    ${first_identifier}    value
    Set Suite Variable   ${input_patient_id}    ${PATIENT_PREFIX}|${resource_id}
    Log To Console       Input Patient Id: ${input_patient_id}

    # Connect To Database to fetch internal_resource_id from fb_resource_composition table
    Connect To Database    psycopg2    ${DB_NAME}    ${DB_USER}    ${DB_PASSWORD}    ${DB_HOST}    ${DB_PORT}
    ${result}=   Query    SELECT internal_resource_id FROM public.fb_resource_composition WHERE input_resource_id = '${input_patient_id}'
    Log To Console    int resource:${result}
    ${result}=   Get From List    ${result}    0
    ${internal_patient_id}=    Get from list    ${result}    0
    Log To Console    \nInternal_resource_id fetched from fb_resource_composition table: ${internal_patient_id}
    Disconnect From Database
    Set Suite Variable   ${patient_id}          ${internal_patient_id}

Find Matching Entry
    [Arguments]         ${reference}    ${entries}
    Log                 Finding entry matching fullUrl: ${reference}
    FOR                 ${entry}    IN    @{entries}
        ${full_url}=    Get From Dictionary    ${entry}    fullUrl
        Run Keyword If  '${full_url}' == '${reference}'    Return From Keyword    ${entry}
    END
    Fail                No matching entry found for fullUrl: ${reference}


validate response - 201
    [Documentation]     Validates response of POST to ${BASE_URL} endpoint
    Integer    response status    201
    Log To Console    \nIn Fhir bundle validate response is 201 Created

create openehr aql
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}
    POST /BundleAQL with ehr reference    ${openehr_template}       ${openehr_canonical_file_name}

POST /BundleAQL with ehr reference
    [Arguments]         ${openehr_template}     ${openehr_canonical_file_name}

    # Connect To Database to fetch ehrId from fb_patient_ehr table
    Connect To Database    psycopg2    ${DB_NAME}    ${DB_USER}    ${DB_PASSWORD}    ${DB_HOST}    ${DB_PORT}
    ${result}=   Query    SELECT ehr_id FROM public.fb_patient_ehr WHERE internal_patient_id = '${patient_id}'
    ${result}=   Get From List    ${result}    0
    ${ehr_id}=    Get from list    ${result}    0
    Log To Console    \nehrId fetched from fb_patient_ehr table: ${ehr_id}
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
    Log To Console    \nUpdated query string: ${updated_query_string}

    # Update the JSON content with the new query string
    ${payload}=      Update Value To Json     ${aql_body}    $.q    ${updated_query_string}
    Log To Console    \nUpdated aql body: ${payload}

    # POST CALL TO GET AQL RESPONSE
    ${HEADERS}    Create Dictionary   Content-Type=application/json   Authorization=Basic ZWhyYmFzZTplaHJiYXNl   Prefer=return=representation
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
    ${expected_resp_composition}=    Load Json Utf8    ${EHR_COMPOSITION}/${expected_openehr_file_name}    ${INPUT_PATIENT_ID}
    # Log To Console    \nEXP Response composition: ${expected_resp_composition}

    ${expected_template_id}=  Get Value From Json    ${expected_resp_composition}    $..archetype_details.template_id.value
    Log To Console    Expected Template ID: ${expected_template_id}\n

#    Should Be Equal    ${expected_template_id}    ${response_template_id}
#    Log To Console    Both Template ID should be equal:
#    Log To Console    Expected Template ID: ${expected_template_id}
#    Log To Console    Response Template ID: ${response_template_id}

    # TODO: For compairing whole json value of expected composition and the response composition
    # Normalize both the JSONs by ignoring value of "value" if "_type" == "DV_DATE_TIME"
    # 'uid' fields entirely
    ${normalized_expected_json}=    Normalize Json    ${expected_resp_composition}
    ${normalized_response_json}=    Normalize Json    ${response_aql_composition}

    # Compare the normalized JSONs returned by the openEHR and the expected cannonical json
    # checking if both json have same sets of key and value pairs other then the ignored fields of comparison .
    #Log To Console    normalized_expected_json: ${normalized_expected_json}
    #Log To Console    normalized_response_json: ${normalized_response_json}
    Should Be Equal    ${normalized_expected_json}    ${normalized_response_json}

Normalize Json
    [Arguments]    ${json_data}
    # get normalized json by calling the method normalize_json with ${json_data} in json_normalizer.py file
    ${normalized_json}=    Evaluate    json_normalizer.normalize_json(${json_data})    modules=json_normalizer
    RETURN    ${normalized_json}
