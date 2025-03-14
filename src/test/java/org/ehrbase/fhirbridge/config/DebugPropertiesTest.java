package org.ehrbase.fhirbridge.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class DebugPropertiesTest {

    private DebugProperties debugProperties;
    private Exchange exchange;
    private ObjectMapper objectMapper;
    private FhirContext fhirContext;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        debugProperties = new DebugProperties();
        debugProperties.setEnabled(true);
        debugProperties.setMappingOutputDirectory(tempDir.toString());
        
        exchange = new DefaultExchange(new DefaultCamelContext());
        objectMapper = new ObjectMapper();
        fhirContext = FhirContext.forR4();

        // Set up common headers
        String openFhirOutcome = "{\"_type\":\"COMPOSITION\",\"name\":{\"_type\":\"DV_TEXT\",\"value\":\"Test\"}}";
        String openEhrOutcome = "{\"archetype_node_id\":\"openEHR-EHR-COMPOSITION.test.v1\"}";
        exchange.getMessage().setHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME, openFhirOutcome);
        exchange.getMessage().setHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME, openEhrOutcome);
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, "Patient");
    }

    @Test
    void testSaveMergedServerResponsesWithResourceOutcome() throws Exception {
        // Create a test Resource
        Patient patient = new Patient();
        patient.setId("test-id");
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, patient);

        debugProperties.saveMergedServerResponses(exchange);

        // Verify output file exists and contains correct content
        Path outputDir = Path.of(debugProperties.getMappingOutputDirectory());
        assertTrue(Files.list(outputDir).findFirst().isPresent());
        
        Path outputFile = Files.list(outputDir).findFirst().get();
        String content = Files.readString(outputFile);
        JsonNode rootNode = objectMapper.readTree(content);
        
        assertTrue(rootNode.has("id"));
        assertEquals("test-id", rootNode.get("id").asText());
    }

    @Test
    void testSaveMergedServerResponsesWithMethodOutcome() throws Exception {
        // Create a test MethodOutcome with Resource
        Bundle bundle = new Bundle();
        bundle.setId("test-bundle-id");
        
        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(bundle);
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, methodOutcome);

        debugProperties.saveMergedServerResponses(exchange);

        // Verify output file exists and contains correct content
        Path outputDir = Path.of(debugProperties.getMappingOutputDirectory());
        assertTrue(Files.list(outputDir).findFirst().isPresent());
        
        Path outputFile = Files.list(outputDir).findFirst().get();
        String content = Files.readString(outputFile);
        JsonNode rootNode = objectMapper.readTree(content);
        
        assertTrue(rootNode.has("id"));
        assertEquals("test-bundle-id", rootNode.get("id").asText());
    }

    @Test
    void testSaveMergedServerResponsesWithStringOutcome() throws Exception {
        // Create a test JSON string
        String jsonString = "{\"resourceType\":\"Patient\",\"id\":\"test-string-id\"}";
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, jsonString);

        debugProperties.saveMergedServerResponses(exchange);

        // Verify output file exists and contains correct content
        Path outputDir = Path.of(debugProperties.getMappingOutputDirectory());
        assertTrue(Files.list(outputDir).findFirst().isPresent());
        
        Path outputFile = Files.list(outputDir).findFirst().get();
        String content = Files.readString(outputFile);
        JsonNode rootNode = objectMapper.readTree(content);
        
        assertTrue(rootNode.has("id"));
        assertEquals("test-string-id", rootNode.get("id").asText());
    }

    @Test
    void testSaveMergedServerResponsesWithInvalidOutcome() {
        // Set an invalid outcome type
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, new Integer(42));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            debugProperties.saveMergedServerResponses(exchange));
        assertTrue(exception.getMessage().contains("Unexpected server response type"));
    }

    @Test
    void testSaveMergedServerResponsesWhenDisabled() throws Exception {
        debugProperties.setEnabled(false);
        
        String jsonString = "{\"resourceType\":\"Patient\",\"id\":\"test-id\"}";
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, jsonString);

        debugProperties.saveMergedServerResponses(exchange);

        // Verify no file was created
        Path outputDir = Path.of(debugProperties.getMappingOutputDirectory());
        assertEquals(0, Files.list(outputDir).count());
    }

    @Test
    void testSaveMergedServerResponsesWithNullOutcome() {
        exchange.setProperty(CamelConstants.FHIR_SERVER_OUTCOME, null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            debugProperties.saveMergedServerResponses(exchange));
        assertTrue(exception.getMessage().contains("Unexpected server response type: null"));
    }
} 