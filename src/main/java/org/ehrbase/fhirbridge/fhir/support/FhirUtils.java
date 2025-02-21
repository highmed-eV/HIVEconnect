package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class FhirUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String ENTRY = "entry";
    public static final String REQUEST = "request";
    public static final String FULL_URL = "fullUrl";
    private static final Logger LOG = LoggerFactory.getLogger(FhirUtils.class);
    
    private FhirUtils() {
        // Private constructor to prevent instantiation
    }

    public static String getResourceType(String resourceJson) {
        try {
            // Parse the JSON
            JsonNode rootNode = objectMapper.readTree(resourceJson);

            // Get resourceType
            if (rootNode.has(RESOURCE_TYPE)) {
                return rootNode.get(RESOURCE_TYPE).asText();
            }

        } catch (Exception e) {
            throw new UnprocessableEntityException("Unable to process the resource JSON and failed to extract resource type");

        }

        return null; // Return null if no patient ID is found
    }

    public static @NotNull List<String> getReferenceResourceIds(String resourceJson) {
        try {
            // Parse the JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(resourceJson);
            List<String> extractedResourceIds;
            Set<String> resultSet = new HashSet<>();

            List<String> fullUrlList = extractFullUrls(rootNode);
            // Handle both Bundle and a single resource
            // Collect all resource IDs from "reference" keys
            extractedResourceIds = extractResourceIds(rootNode, fullUrlList);

            if (!extractedResourceIds.isEmpty()) {
                // Collect all the uniques resource IDs
                resultSet.addAll(extractedResourceIds);
                // List of all the unique resource Ids in the input bundle or single resource
                return new ArrayList<>(resultSet);
            }
        } catch (Exception e) {
            throw new UnprocessableEntityException("Unable to process the resource JSON and failed to extract reference resource IDs");
        }

        return Collections.emptyList(); // Return an empty list if no resource ID is found
    }

    private static List<String> extractResourceIds(JsonNode resourceNode, List<String> fullUrlList) {
        List<String> resourceIds = new ArrayList<>();
        String regex = "^[^/]+/[A-Za-z0-9-]+$";

        if (resourceNode.isObject()) {
            extractFromObject(resourceNode, fullUrlList, regex, resourceIds);
        } else if (resourceNode.isArray()) {
            extractFromArray(resourceNode, fullUrlList, resourceIds);
        }
        return resourceIds;
    }

    private static void extractFromObject(JsonNode resourceNode, List<String> fullUrlList, String regex, List<String> resourceIds) {
        Iterator<String> fieldNames = resourceNode.fieldNames();
        boolean referenceProcessed = false;

        while (fieldNames.hasNext() && !referenceProcessed) {
            String fieldName = fieldNames.next();
            JsonNode childNode = resourceNode.get(fieldName);
            // Skip the "subject" field
            if ("subject".equals(fieldName)) {
                continue;
            }
            // If "reference" key is found, extract the ID
            if ("reference".equals(fieldName) && childNode.isTextual()) {
                String reference = childNode.asText();
                if (Pattern.matches(regex, reference) || (reference.startsWith("urn:uuid") && !fullUrlList.contains(reference))) {
                    // Extract resource ID from the reference value
                    resourceIds.add(reference);
                    referenceProcessed = true;
                }
            } else {
                // Recursively process child nodes
                resourceIds.addAll(extractResourceIds(childNode, fullUrlList));
            }
        }
    }

    private static void extractFromArray(JsonNode resourceNode, List<String> fullUrlList, List<String> resourceIds) {
        for (JsonNode arrayElement : resourceNode) {
            resourceIds.addAll(extractResourceIds(arrayElement, fullUrlList));
        }
    }

    private static List<String> extractFullUrls(JsonNode rootNode) {
        List<String> fullUrls = new ArrayList<>();

        // Directly target the `entry` array
        if (rootNode.has(ENTRY) && rootNode.get(ENTRY).isArray()) {
            for (JsonNode entry : rootNode.get(ENTRY)) {
                if (entry.has(FULL_URL) && entry.get(FULL_URL).isTextual()) {
                    fullUrls.add(entry.get(FULL_URL).asText());
                }
            }
        }
        return fullUrls;
    }

    public static String serializeOperationOutcome(IBaseOperationOutcome outcome) {
        return FHIR_CONTEXT.newJsonParser().encodeResourceToString(outcome);
    }

    public static @NotNull List<String> getInputResourceIds(String resourceJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(resourceJson);
            Set<String> inputResourceIds = new HashSet<>();

            if (rootNode != null && rootNode.isObject()) {
                if (rootNode.has(RESOURCE_TYPE) && "Bundle".equals(rootNode.get(RESOURCE_TYPE).asText())) {
                    // Handle as a Bundle
                    extractFromBundle(rootNode, inputResourceIds);
                } else {
                    // Handle as a single resource
                    if (rootNode.has(RESOURCE_TYPE) && rootNode.has("id")) {
                        String resourceType = rootNode.get(RESOURCE_TYPE).asText();
                        String id = rootNode.get("id").asText();
                        inputResourceIds.add(resourceType + "/" + id);
                    }
                }
            }
            if (!inputResourceIds.isEmpty()) {
                // Return List of all the unique resource Ids in the input bundle or single resource
                return new ArrayList<>(inputResourceIds);
            }
        } catch (Exception e) {
            throw new UnprocessableEntityException("Unable to process the resource JSON and failed to extract resource IDs from the provided JSON");
        }

        return Collections.emptyList(); // Return an empty list if no resource ID is found
    }

    private static void extractFromBundle(JsonNode rootNode, Set<String> inputResourceIds) {
        JsonNode entries = rootNode.path(ENTRY);
        for (JsonNode entry : entries) {
            JsonNode resource = entry.path("resource");
            if (resource.has(RESOURCE_TYPE) && resource.has("id")) {
                String resourceType = resource.get(RESOURCE_TYPE).asText();
                String id = resource.get("id").asText();
                inputResourceIds.add(resourceType + "/" + id);
            }
        }
    }

    public static void extractRequestTypeFromFromBundle(Exchange exchange) {
        String inputResource = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
        try{
            JsonNode rootNode = objectMapper.readTree(inputResource);
            
            var methods = StreamSupport.stream(rootNode.spliterator(), false)
                            .map(node -> {
                                JsonNode requests = node.path("request");
                                return requests.has("method") ? requests.get("method").asText() : null;
                            })
                            .distinct()
                            .toList();
            
            if (methods.contains(null) || methods.size() > 1) {
                throw new IllegalArgumentException("Inconsistent or invalid request methods detected");
            }
            exchange.getIn().setHeader(CamelConstants.INPUT_OPERATION_TYPE, methods.get(0));
        
        } catch (JsonProcessingException e) {
            throw new UnprocessableEntityException("Unable to process the resource JSON and failed to extract resource type");
        }   
    }
}


