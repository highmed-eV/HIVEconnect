package org.ehrbase.fhirbridge.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@ConfigurationProperties(prefix = "fhir-bridge.debug")
public class DebugProperties {

    private boolean enabled = false;

    private String mappingOutputDirectory;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMappingOutputDirectory() {
        return mappingOutputDirectory;
    }

    public void setMappingOutputDirectory(String mappingOutputDirectory) {
        this.mappingOutputDirectory = mappingOutputDirectory;
    }

    public void saveMergedServerResponses(Exchange exchange) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Parse JSON strings into JsonNode objects
        JsonNode node1 = objectMapper.readTree((String) exchange.getProperty(CamelConstants.FHIR_SERVER_OUTCOME));
        JsonNode node2 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_FHIR_SERVER_OUTCOME));
        JsonNode node3 = objectMapper.readTree((String) exchange.getMessage().getHeader(CamelConstants.OPEN_EHR_SERVER_OUTCOME));

        // Merge JsonNode objects into a single ObjectNode
        ObjectNode mergedJson = objectMapper.createObjectNode();
        mergedJson.setAll((ObjectNode) node1);
        mergedJson.setAll((ObjectNode) node2);
        mergedJson.setAll((ObjectNode) node3);

        String inputResourceType = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE_TYPE);

        Files.createDirectories(Paths.get(mappingOutputDirectory));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = inputResourceType + "_" + timestamp + ".json";
        String filePath = mappingOutputDirectory + "/" + filename;

        Files.write(Paths.get(filePath), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(mergedJson), StandardOpenOption.CREATE);
    }
}
