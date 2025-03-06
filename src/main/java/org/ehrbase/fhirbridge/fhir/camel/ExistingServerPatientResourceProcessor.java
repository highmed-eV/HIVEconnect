package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component(ExistingServerPatientResourceProcessor.BEAN_ID)
public class ExistingServerPatientResourceProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "existingServerPatientResourceProcessor";

    @Override
    public void process(Exchange exchange) throws Exception {

        if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
            Patient patientResource = (Patient) exchange.getIn().getBody();
            String serverPatientId = patientResource.getId();
            exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_RESOURCE, patientResource);
            exchange.getIn().setHeader(CamelConstants.SERVER_PATIENT_ID, serverPatientId);
        }
    }
}
