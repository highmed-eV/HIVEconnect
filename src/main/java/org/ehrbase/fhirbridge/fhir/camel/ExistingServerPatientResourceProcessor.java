package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Component(ExistingServerPatientResourceProcessor.BEAN_ID)
public class ExistingServerPatientResourceProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "existingServerPatientResourceProcessor";

    @Override
    public void process(Exchange exchange) throws Exception {

        if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
            Patient patientResource = (Patient) exchange.getIn().getBody();
            String serverPatientId = patientResource.getId();
            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE, patientResource);
            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_ID, serverPatientId);
        } else {
            if (exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID_TYPE).equals("SEARCH_URL")){
                throw new UnprocessableEntityException("Patient not found for search url: " + exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_SEARCH_URL));
            }
            //In case of RELATIVE_REFERENCE, the patient resource will be created in  the server
        }
    }
}
