package org.highmed.hiveconnect.camel.route;

import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.openehr.sdk.util.exception.ClientException;
import org.highmed.hiveconnect.exception.HiveConnectExceptionHandler;
import org.highmed.hiveconnect.exception.OpenEhrClientExceptionHandler;
import org.springframework.stereotype.Component;


@Component
public class ProvideResourceRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route to perform the ETL for the FHIR request
        from("direct:provideResource")
            //Perform Extract, Validate and Enrich
            .to("direct:PatientReferenceProcessor")
            .to("direct:ResourcePersistenceProcessor")
            // Step 5: Extract Patient Id created in the FHIR server
            .to("direct:extractPatientIdFromFhirResponseProcessor")

            //Perform Transform and load in case of non Patient resource
            //Skip Transform and load in case of Patient resource
            .choice()
                .when(simple("${header.CamelRequestResourceResourceType} == 'Patient'"))
                    .doTry()
                        //Get the mapped openEHRId if available else create new ehrId
                        .to("direct:patientIdToEhrIdMapperProcess")
                    .doCatch(Exception.class)
                        .log("direct:handleCreateOperationETL exception")
                        .process(new HiveConnectExceptionHandler())
                    .endDoTry()
                .endChoice()
            .otherwise()
                .doTry()
                    .to("direct:send-to-cdr")
                .doCatch(ClientException.class)
                    .process(new OpenEhrClientExceptionHandler())
                .endDoTry()
                .endChoice()
            .end();
        }
}