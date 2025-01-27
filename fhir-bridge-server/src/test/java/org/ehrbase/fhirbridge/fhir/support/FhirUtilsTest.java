package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getResourceTypeValidJson() throws Exception {
        String resourceJson = "{\"resourceType\":\"Patient\"}";
        String resourceType = FhirUtils.getResourceType(resourceJson);
        assertEquals("Patient", resourceType);
    }

    @Test
    void getResourceTypeInvalidJson() {
        String invalidJson = "invalid";
        String resourceType = FhirUtils.getResourceType(invalidJson);
        assertNull(resourceType);
    }

    @Test
    void getResourceIdsBundleResource() throws Exception {
        String bundleJson = "{\"resourceType\":\"Bundle\",\"entry\":[{\"fullUrl\":\"urn:uuid:123\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"123\",\"encounter\":{\"reference\":\"Encounter/6\"},\"organization\":{\"reference\":\"Organization/7\"}}},{\"fullUrl\":\"urn:uuid:456\",\"resource\":{\"resourceType\":\"Observation\",\"id\":\"456\",\"encounter\":{\"reference\":\"Encounter/6\"},\"organization\":{\"reference\":\"Organization/7\"}}}]}";
        List<String> resourceIds = FhirUtils.getResourceIds(bundleJson);
        assertNotNull(resourceIds);
        assertTrue(resourceIds.contains("Encounter/6"));
        assertTrue(resourceIds.contains("Organization/7"));
    }

    @Test
    void getResourceIdsNonBundleResource() throws Exception {
        String resourceJson = "{\"resourceType\":\"Patient\",\"id\":\"123\",\"generalPractitioner\":[{\"reference\":\"Practitioner/456\"}],\"managingOrganization\":{\"reference\":\"Organization/789\"}}";
        List<String> resourceIds = FhirUtils.getResourceIds(resourceJson);
        assertNotNull(resourceIds);
        assertTrue(resourceIds.contains("Practitioner/456"));
        assertTrue(resourceIds.contains("Organization/789"));
    }
}

