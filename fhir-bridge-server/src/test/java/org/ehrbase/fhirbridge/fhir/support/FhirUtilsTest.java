package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FhirUtilsTest {

    @Mock
    private IBaseOperationOutcome outcome;

    @Mock
    private IParser parser;

    @Mock
    private FhirContext fhirContextMock;

    @Test
    void getResourceTypeValidJson() {
        String resourceJson = "{\"resourceType\":\"Patient\"}";
        String resourceType = FhirUtils.getResourceType(resourceJson);
        assertEquals("Patient", resourceType);
    }

    @Test
    void getResourceTypeInvalidJson() {
        String invalidJson = "invalid";

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () -> {
            FhirUtils.getResourceType(invalidJson);
        });

        assertEquals("Unable to process the resource JSON and failed to extract resource type", exception.getMessage());
    }

    @Test
    void getResourceIdsBundleReferenceResource() {
        String bundleJson = "{\"resourceType\":\"Bundle\",\"entry\":[{\"fullUrl\":\"urn:uuid:123\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"123\",\"encounter\":{\"reference\":\"Encounter/6\"},\"organization\":{\"reference\":\"Organization/7\"}}},{\"fullUrl\":\"urn:uuid:456\",\"resource\":{\"resourceType\":\"Observation\",\"id\":\"456\",\"encounter\":{\"reference\":\"Encounter/6\"},\"organization\":{\"reference\":\"Organization/7\"}}}]}";
        List<String> resourceIds = FhirUtils.getReferenceResourceIds(bundleJson);
        assertNotNull(resourceIds);
        assertTrue(resourceIds.contains("Encounter/6"));
        assertTrue(resourceIds.contains("Organization/7"));
    }

    @Test
    void getResourceIdsNonBundleReferenceResource() {
        String resourceJson = "{\"resourceType\":\"Patient\",\"id\":\"123\",\"generalPractitioner\":[{\"reference\":\"Practitioner/456\"}],\"managingOrganization\":{\"reference\":\"Organization/789\"}}";
        List<String> resourceIds = FhirUtils.getReferenceResourceIds(resourceJson);
        assertNotNull(resourceIds);
        assertTrue(resourceIds.contains("Practitioner/456"));
        assertTrue(resourceIds.contains("Organization/789"));
    }

    @Test
    void getInputResourceIdsFromBundle() {
        String bundleJson = "{\"resourceType\":\"Bundle\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"123\"}},{\"resource\":{\"resourceType\":\"Observation\",\"id\":\"456\"}}]}";
        List<String> resourceIds = FhirUtils.getInputResourceIds(bundleJson);
        assertNotNull(resourceIds);
        assertEquals(2, resourceIds.size());
        assertTrue(resourceIds.contains("Patient/123"));
        assertTrue(resourceIds.contains("Observation/456"));
    }
}

