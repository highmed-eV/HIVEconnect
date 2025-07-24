package org.highmed.hiveconnect.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Exchange;
import org.highmed.hiveconnect.camel.CamelConstants;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@ConfigurationProperties(prefix = "hive-connect.debug")
@Setter
@Getter
public class DebugProperties {

    private boolean enabled = false;

    private String mappingOutputDirectory;

    private static final String PATH_DELIMITER = "/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveMergedServerResponses(Exchange exchange) throws IOException {

        if (!enabled) {
            return;
        }

        // Parse JSON strings into JsonNode objects
        Object outcome = exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME);
        String jsonString;
        if (outcome instanceof Resource) {
            jsonString = FhirContext.forR4().newJsonParser().encodeResourceToString((Resource) outcome);
        } else if (outcome instanceof MethodOutcome) {
            Resource resource = (Resource) ((MethodOutcome) outcome).getResource();
            jsonString = FhirContext.forR4().newJsonParser().encodeResourceToString(resource);
        } else if (outcome instanceof String) {
            jsonString = (String) outcome;
        } else {
            throw new IllegalArgumentException("Unexpected server response type: " + 
                (outcome != null ? outcome.getClass().getName() : "null"));
        }
        JsonNode node1 = objectMapper.readTree(jsonString);
        JsonNode node2 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME));
        JsonNode node3 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME));

        // Merge JsonNode objects into a single ObjectNode
        ObjectNode mergedJson = objectMapper.createObjectNode();
        mergedJson.set("fhirOutcome", node1);
        mergedJson.set("openFhirOutcome", node2);
        mergedJson.set("openEhrOutcome", node3);


        String inputResourceType = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE);

        Files.createDirectories(Paths.get(mappingOutputDirectory));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = inputResourceType + "_" + timestamp + ".json";
        String filePath = mappingOutputDirectory + PATH_DELIMITER + filename;

        Files.write(Paths.get(filePath), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(mergedJson), StandardOpenOption.CREATE);
    }
}
