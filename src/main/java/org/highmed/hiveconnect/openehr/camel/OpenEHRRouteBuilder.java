package org.highmed.hiveconnect.openehr.camel;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.Optional;

import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.openehr.sdk.util.exception.ClientException;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.highmed.hiveconnect.camel.component.ehr.composition.CompositionConstants;
import org.highmed.hiveconnect.exception.OpenEhrClientExceptionHandler;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
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
                //set the compositionId if the method is put
                if("PUT".equals(exchange.getIn().getHeader(CamelConstants.REQUEST_HTTP_METHOD))){
                    String compositionId = (String) exchange.getMessage().getHeader(CamelConstants.OPENEHR_COMPOSITION_ID);
                    Optional.ofNullable(compositionId)
                        .ifPresent(id -> composition.setUid(new ObjectVersionId(id)));
                }
                exchange.getIn().setBody(composition);
            })

            //populate conext meta data information(Composer, feeder audit, languate, territory) in the composition
            .process(CompositionContextProcessor.BEAN_ID)

            .doTry()
                .to("ehr-composition:compositionProducer?operation=mergeCanonicalCompositionEntity")
                .log("Successfully committed composition to openEHR server with EHR ID: ${header." + CompositionConstants.EHR_ID + "}")
            .doCatch(ClientException.class)
                .log("composition:compositionProducer exception")
                //TODO: Rollback
                // .to("direct:deleteResources")
                .process(new OpenEhrClientExceptionHandler())
            .endDoTry();
 

        from("direct:patientIdToEhrIdMapperProcess")
            //Get the mapped openEHRId if avaialbe else create new ehrId 
            .routeId("patientIdToEhrIdMapperProcessRoute")

            .doTry()
                //if Patient resource create ehrid
                .log("Calling EhrLookupProcessor")
                .process(EhrLookupProcessor.BEAN_ID)
            .doCatch(ClientException.class)
                .log("EhrLookupProcessor exception")
                .process(new OpenEhrClientExceptionHandler())
            .endDoTry()
            .log("Patient ID mapped to EHR ID: ${header.CamelEhrCompositionEhrId}");
    }
}

