package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
public class FhirUtils {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    public static final String ENTRY = "entry";
    public static final String FULL_URL = "fullUrl";
    
    private FhirUtils() {
        // Private constructor to prevent instantiation
    }

    public static String getResourceType(Resource resource) {
        try {
            return resource.getResourceType().name();
        } catch (Exception e) {
            throw new UnprocessableEntityException("Unable to process the resource JSON and failed to extract resource type");
        }
    }

    public static @NotNull List<String> getReferenceResourceIds(Resource resource) {
        try {
            //TODO: change to fhir built in functions to parse the resource
            String resourceJson = FHIR_CONTEXT.newJsonParser().encodeResourceToString(resource);
            // Parse the JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(resourceJson);
            List<String> extractedResourceIds;

            List<String> fullUrlList = extractFullUrls(rootNode);
            // Handle both Bundle and a single resource
            // Collect all resource IDs from "reference" keys
            extractedResourceIds = extractResourceIds(rootNode, fullUrlList);

            if (!extractedResourceIds.isEmpty()) {
                // Collect all the uniques resource IDs
                Set<String> resultSet = new HashSet<>(extractedResourceIds);
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

    public static @NotNull List<String> getInputResourceIds(Resource resource) {
        Set<String> inputResourceIds = new HashSet<>();

        if (resource instanceof Bundle) {
            // Handle as a Bundle
            extractFromBundle((Bundle) resource, inputResourceIds);
        } else {
            // Handle as a single resource
            if (resource.hasId()) {
                inputResourceIds.add(resource.getResourceType().name() + "/" + resource.getIdElement().getIdPart());
            }
        }

        return new ArrayList<>(inputResourceIds);
    }

    private static void extractFromBundle(Bundle bundle, Set<String> inputResourceIds) {
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            if (resource != null && resource.hasId()) {
                inputResourceIds.add(resource.getResourceType().name() + "/" + resource.getIdElement().getIdPart());
            }
        }
    }

    public static void extractInputParameters(Exchange exchange) {
        Resource inputResource = (Resource) exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE);
        String inputResourceType = getResourceType(inputResource);
        String method;
        List<String> profiles;

        if ("Bundle".equals(inputResourceType)) {
            Bundle bundle = (Bundle) inputResource;
            
            // Get distinct request methods
            var methods = bundle.getEntry().stream()
                    .map(entry -> entry.getRequest() != null ? entry.getRequest().getMethod().toCode() : null)
                    .distinct()
                    .toList();
            
            if (methods.contains(null) || methods.size() > 1) {
                throw new IllegalArgumentException("Inconsistent or invalid request http methods detected");
            }
            method = methods.get(0);

            // Extract profiles from resources' meta
            profiles = bundle.getEntry().stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(entryResource -> entryResource != null && entryResource.getMeta() != null)
                    .flatMap(entryResource -> entryResource.getMeta().getProfile().stream())
                    .map(PrimitiveType::getValue)
                    .distinct()
                    .toList();
            
        } else {
            // Get method from exchange property
            method = exchange.getProperty(Exchange.HTTP_METHOD, String.class);

            // Extract profiles directly from resource meta
            profiles = Optional.ofNullable(inputResource.getMeta())
                    .map(meta -> meta.getProfile().stream()
                            .map(PrimitiveType::getValue)
                            .toList())
                    .orElse(Collections.emptyList());
        }

        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("Meta profile not provided in the resource");
        }

        // Get source from meta
        String metaSource = Optional.ofNullable(inputResource.getMeta())
                .map(Meta::getSource)
                .orElse(null);
        
        // Set input parameters in exchange
        if (metaSource != null) {
            exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_SOURCE, metaSource);
        }
        
        // Set headers
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE, inputResource);
        exchange.getIn().setHeader(CamelConstants.REQUEST_RESOURCE_TYPE, inputResourceType);
        exchange.getIn().setHeader(CamelConstants.REQUEST_HTTP_METHOD, method);
        exchange.getIn().setHeader(CamelConstants.FHIR_INPUT_PROFILE, profiles);
    }
}


