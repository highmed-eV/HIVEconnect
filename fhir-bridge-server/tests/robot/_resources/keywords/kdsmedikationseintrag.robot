*** Keywords ***
validate response - 201
    Log To Console    In Fhir bundle validate response

validate content response_aql - 201
    ${template_id}=    Get Value From Json    ${response_aql}    $.archetype_details.template_id.value
    Log To Console    In openEHR AQL validate response
    Log To Console    The template_id is ${template_id}[0]
    Should Be Equal As Strings    ${template_id}[0]    KDS_Medikationseintrag_v1

create fhir bundle
    [Arguments]         ${text}    ${example_json}
    POST /KDSMedikationseintragBundle with ehr reference    ${text}    ${example_json}


POST /KDSMedikationseintragBundle with ehr reference
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
     ${payload}         Load JSON From File    ${DATA_SET_PATH_KDSFHIRBUNDLE}/${fhir_bundle_file_name}
     Log To Console    Fhir bundle API call
     #    POST CALL TO STORE THE FHIR BUNDLE
     #    &{resp}             POST    ${BASE_URL}/KDSMedikationseintragBundleAQL    body=${payload}
     #                        Output Debug Info To Console

create openehr aql
    [Arguments]         ${openehr_template}
    POST /KDSMedikationseintragBundleAQL with ehr reference    ${openehr_template}

POST /KDSMedikationseintragBundleAQL with ehr reference
    [Arguments]         ${openehr_template}
    #    replace the template id in the aql and pass to openehr cdr
    #    POST CALL TO GET AQL RESPONSE
    #    &{resp_ehr}             POST    ${BASE_URL}/ehrbase/rest/openehr/v1/query/aql    body=${payload}
    #                    Output Debug Info To Console
    #    currently mocking the aql response from the file
    &{resp_ehr}=    Load JSON From File    ${EHR_COMPOSITION}/kds_medikationseintrag_v1_canonical.json
    Log To Console    In openEHR AQL API call
                    Set Suite Variable    ${response_aql}    ${resp_ehr}
