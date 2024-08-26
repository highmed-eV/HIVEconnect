*** Settings ***
Library    REST

*** Keywords ***
validate content response_aql - 201
        ${template_id}=    Get Value From Json    ${response_aql}    $.archetype_details.template_id.value
        Log To Console    The template_id is ${template_id}[0]
        Should Be Equal As Strings    ${template_id}[0]    KDS_Person


