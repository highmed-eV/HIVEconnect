package org.ehrbase.fhirbridge.fhir.camel;

import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.springframework.stereotype.Component;
import org.hl7.fhir.r4.model.Patient;

@Component(ServerPatientResourceProcessor.BEAN_ID)
public class ServerPatientResourceProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "serverPatientResourceProcessor";

    @Override
    public void process(Exchange exchange) throws Exception {

        if (ObjectHelper.isNotEmpty(exchange.getIn().getBody())) {
            Patient patientResource = (Patient) exchange.getIn().getBody();
            exchange.getIn().setHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE, patientResource);
        }
    }
}
