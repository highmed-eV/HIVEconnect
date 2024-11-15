package org.ehrbase.fhirbridge.fhir.bundle.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;

import org.ehrbase.fhirbridge.fhir.common.Profile;
import org.ehrbase.fhirbridge.fhir.support.Bundles;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converter for 'BloodBasPanel' profile that convert a Bundle in an Observation with contained resources.
 */
public class DefaultBundleToResourceConverter extends AbstractBundleConverter<DomainResource> {

    private static final Logger log = LoggerFactory.getLogger(DefaultBundleToResourceConverter.class);

    @Override
    public DomainResource convert(@NonNull Bundle bundle) {
        DomainResource domainResource = getRoot(bundle);
        Map<String, Resource> resources = mapResources(bundle);

        List<Resource> contains = new ArrayList<>();
        List<Reference> references = extractReferences(domainResource);

        // Extract 'hasMember' references
        // List<Reference> hasMemberReferences = extractHasMemberReferences(bundle);

        for (Reference reference : references) {
            Resource resource = resources.get(reference.getReference());
            if (resource == null) {
                log.info("Resource not found for " + reference.getReference());
                continue;
                // throw new UnprocessableEntityException("Resource '" + reference.getReference() + "' is missing");
            }

            log.info("Contained resource: " + reference.getReference());
            resource.setId((String) null);
            reference.setReference(null);
            reference.setResource(resource);
            contains.add(resource);
        }


        if( contains.size() != 0) {
            // TODO: Do we have to process all elements here?
            domainResource.setContained(contains);
        }
        return domainResource;
    }

    public static List<Reference> extractHasMemberReferences(Bundle bundle) {
        // Initialize list to collect references
        List<Reference> allHasMemberReferences = new ArrayList<>();

        // Iterate through all entries in the bundle
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof DomainResource) {
                DomainResource resource = (DomainResource) entry.getResource();

                // Check if the resource has a "getHasMember" method using reflection
                try {
                    Method hasMemberMethod = resource.getClass().getMethod("getHasMember");
                    @SuppressWarnings("unchecked")
                    List<Reference> hasMembers = (List<Reference>) hasMemberMethod.invoke(resource);

                    if (hasMembers != null) {
                        allHasMemberReferences.addAll(hasMembers);
                    }
                } catch (Exception e) {
                    // Method not found, ignore this resource
                    // This will occur if the resource does not have a hasMember field
                }
            }
        }

        return allHasMemberReferences;
    }


    public static List<Reference> extractReferences(DomainResource resource) {
        // Create a FhirContext for R4
        FhirContext ctx = FhirContext.forR4();

        // Get the Terser object to extract elements
        FhirTerser terser = ctx.newTerser();

        // Get all populated elements of type Reference
        List<Reference> elements = terser.getAllPopulatedChildElementsOfType(resource, Reference.class);

        // Cast IBase elements to Reference and collect them in a list
        return elements.stream()
                .filter(element -> element instanceof Reference)
                .map(element -> (Reference) element)
                .collect(Collectors.toList());
    }
}

