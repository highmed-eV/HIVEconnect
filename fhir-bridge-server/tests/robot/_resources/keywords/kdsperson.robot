*** Keywords ***
validate content response_aql - 201
    ${template_id}=    Get Value From Json    ${response_aql}    $.archetype_details.template_id.value
    Log To Console    The template_id is ${template_id}[0]
    Should Be Equal As Strings    ${template_id}[0]    KDS_Person

create fhir bundle
    [Arguments]         ${text}    ${example_json}
    POST /KDSPersonBundle with ehr reference    ${text}    ${example_json}


POST /KDSPersonBundle with ehr reference
    [Arguments]         ${fhir_bundle_name}    ${fhir_bundle_file_name}
     ${payload}         Load JSON From File    ${DATA_SET_PATH_KDSFHIRBUNDLE}/${fhir_bundle_file_name}
#     POST CALL TO STORE THE FHIR BUNDLE
                        POST /KDSPersonBundleAQL    ${payload}  # ${payload}  is dummy arugument that is passed but never used

POST /KDSPersonBundleAQL
    [Arguments]         ${aql-kds-medikamentenverabreichungen}
    &{resp_ehr}=    Load JSON From File    ${KDS_PRESON_EHR_COMPOSITION}/kds-person-canonical.json
                    Set Suite Variable    ${response_aql}    ${resp_ehr}
