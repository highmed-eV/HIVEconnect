package org.ehrbase.fhirbridge.camel.route;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import com.nedap.archie.rm.composition.Composition;
import org.apache.camel.builder.RouteBuilder;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.exception.OpenEhrClientExceptionHandler;
import org.ehrbase.fhirbridge.openehr.camel.EhrLookupProcessor;
import org.ehrbase.fhirbridge.openehr.camel.ProvideResourceResponseProcessor;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S1192")
public class OpenEHRRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        from("direct:OpenEHRProcess")
            // Step 2: Get the mapped openEHRId if avaialbe else create new ehrId 
            .to("direct:patientIdToEhrIdMapperProcess")
            .log("Patient ID mapped to EHR ID: ${exchangeProperty.CamelEhrCompositionEhrId}")
    
            // Step 5: Commit the openEHR composition to the openEHR server
            .process(exchange -> {
                String openEhrJson = exchange.getIn().getBody(String.class);
                UUID ehrId = exchange.getIn().getHeader(CompositionConstants.EHR_ID, UUID.class); // Retrieve EHRId from exchange header
                // Pass the EHRId with the openEHR composition
                // hardcoded correct openEhrJson
                // String filePath = "C:\\FHIR-OpenEHR\\fhir-bridge\\fhir-bridge-server\\src\\main\\resources\\test_canonical_json\\KDS_Diagnose.json";
                // String openEhrJson = new String(Files.readAllBytes(Paths.get(filePath)));
                Composition composition = new CanonicalJson().unmarshal(openEhrJson, Composition.class);
                exchange.getIn().setBody(composition);
            })
            .doTry()
                .to("ehr-composition:compositionProducer?operation=mergeCanonicalCompositionEntity")
                // .to("bean:fhirBridgeOpenEHRAdapter?method=commitComposition")
            .doCatch(ClientException.class)
                .process(new OpenEhrClientExceptionHandler())
            .end()
            .log("Successfully committed composition to openEHR server with EHR ID: ${header.EHRId}");


        // Entry point: Receive a FHIR resource or bundle
        from("direct:patientIdToEhrIdMapperProcess")
            .routeId("patientIdToEhrIdMapperProcess")
            //if Patient resource create ehrid
            .log("patientIdToEhrIdMapperProcess: calling EhrLookupProcessor")
            .process(EhrLookupProcessor.BEAN_ID)
            
            // // Step 1: Extract or find the Patient ID from the resource
            // .process(exchange -> {
                
            //     String patientId = exchange.getProperty("FHIRPatientId", String.class);
            //     //lookup db for mapping
            //     //return if found
            //     //else create ehrid
            //     exchange.setProperty("OpenEHRId", "9dd5ea8b-d9ed-4449-951d-6db59ca23fba");
            // })

            // Step 2: Check if the patient exists in the HAPI FHIR server
            // .choice()
            //     .when(simple("${exchangeProperty.OpenEHRId} == null"))
            //         .toD("direct:openEHRIdCreateProcess")
                    
            .log("openEHR EHRId Mapper Process Route completed: ${exchangeProperty.OpenEHRId}");
    }
}

