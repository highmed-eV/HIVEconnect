package org.ehrbase.fhirbridge.camel.route;

import com.nedap.archie.rm.composition.Composition;
import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.exception.OpenEhrClientExceptionHandler;
import org.ehrbase.fhirbridge.openehr.camel.EhrLookupProcessor;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S1192")
public class OpenEHRRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        from("direct:OpenEHRProcess")
            //Commit the openEHR composition to the openEHR server
            .process(exchange -> {
                String openEhrJson = exchange.getIn().getBody(String.class);
                Composition composition = new CanonicalJson().unmarshal(openEhrJson, Composition.class);
                exchange.getIn().setBody(composition);
            })
            .doTry()
                .to("ehr-composition:compositionProducer?operation=mergeCanonicalCompositionEntity")
            .doCatch(ClientException.class)
                .log("composition:compositionProducer catch exception")
                .to("direct:deleteResources")
                .process(new OpenEhrClientExceptionHandler())
            .endDoTry()
            .log("Successfully committed composition to openEHR server with EHR ID: ${header." + CompositionConstants.EHR_ID + "}");


        from("direct:patientIdToEhrIdMapperProcess")
            //Get the mapped openEHRId if avaialbe else create new ehrId 
            .routeId("patientIdToEhrIdMapperProcess")

            .doTry()
                //if Patient resource create ehrid
                .log("patientIdToEhrIdMapperProcess: calling EhrLookupProcessor")
                .process(EhrLookupProcessor.BEAN_ID)
            .doCatch(ClientException.class)
                .log("patientIdToEhrIdMapperProcess catch exception")
                .process(new OpenEhrClientExceptionHandler())
            .endDoTry()

            .log("openEHR EHRId Mapper Process Route completed: ${header." + CompositionConstants.EHR_ID + "}");
    }
}

