package org.ehrbase.fhirbridge.fhir.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.hl7.fhir.r4.model.*;

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

    private final FhirContext fhirContext = FhirContext.forR4();
    private final Exchange exchange = new DefaultExchange(new DefaultCamelContext());

    @Test
    void getResourceTypeValidResource() {
        Patient patient = new Patient();
        assertEquals("Patient", FhirUtils.getResourceType(patient));
    }

    @Test
    void getResourceTypeInvalidResource() {
        Resource nullResource = null;
        assertThrows(UnprocessableEntityException.class, () -> 
            FhirUtils.getResourceType(nullResource));
    }

    @Test
    void getResourceTypeWithDifferentResourceTypes() {
        Observation observation = new Observation();
        assertEquals("Observation", FhirUtils.getResourceType(observation));

        Organization organization = new Organization();
        assertEquals("Organization", FhirUtils.getResourceType(organization));

        Bundle bundle = new Bundle();
        assertEquals("Bundle", FhirUtils.getResourceType(bundle));
    }

    @Test
    void getResourceIdsBundleReferenceResource() {
        Bundle bundle = new Bundle();
        
        // Add first entry with Patient
        Patient patient = new Patient();
        patient.setId("123");
        Reference encounterRef = new Reference("Encounter/6");
        patient.addContained(new Encounter().setId("6"));
        patient.getManagingOrganization().setReference("Organization/7");
        bundle.addEntry().setResource(patient);
        
        // Add second entry with Observation
        Observation obs = new Observation();
        obs.setId("456");
        obs.getEncounter().setReference("Encounter/6");
        obs.addPerformer().setReference("Organization/7");
        bundle.addEntry().setResource(obs);

        List<String> resourceIds = FhirUtils.getReferenceResourceIds(obs);
        
        assertNotNull(resourceIds);
        assertEquals(2, resourceIds.size());
        assertTrue(resourceIds.contains("Encounter/6"));
        assertTrue(resourceIds.contains("Organization/7"));
    }

    @Test
    void getResourceIdsNonBundleReferenceResource() {
        Patient patient = new Patient();
        patient.setId("123");
        patient.addGeneralPractitioner().setReference("Practitioner/456");
        patient.getManagingOrganization().setReference("Organization/789");

        List<String> resourceIds = FhirUtils.getReferenceResourceIds(patient);
        
        assertNotNull(resourceIds);
        assertEquals(2, resourceIds.size());
        assertTrue(resourceIds.contains("Practitioner/456"));
        assertTrue(resourceIds.contains("Organization/789"));
    }

    @Test
    void getInputResourceIdsFromBundle() {
        Bundle bundle = new Bundle();
        
        // Add multiple resources with IDs
        Patient patient = new Patient();
        patient.setId("test-patient-id");
        bundle.addEntry().setResource(patient);

        Observation obs = new Observation();
        obs.setId("test-obs-id");
        bundle.addEntry().setResource(obs);

        List<String> resourceIds = FhirUtils.getInputResourceIds(bundle);
        assertEquals(2, resourceIds.size());
        assertTrue(resourceIds.contains("Patient/test-patient-id"));
        assertTrue(resourceIds.contains("Observation/test-obs-id"));
    }

    @Test
    void getInputResourceIdsFromSingleResource() {
        // Test with a resource that has an ID
        Observation obs = new Observation();
        obs.setId("test-obs-id");
        List<String> resourceIds = FhirUtils.getInputResourceIds(obs);
        assertEquals(1, resourceIds.size());
        assertEquals("Observation/test-obs-id", resourceIds.get(0));

        // Test with a different resource type
        Patient patient = new Patient();
        patient.setId("test-patient-id");
        resourceIds = FhirUtils.getInputResourceIds(patient);
        assertEquals(1, resourceIds.size());
        assertEquals("Patient/test-patient-id", resourceIds.get(0));
    }

    @Test
    void getInputResourceIdsWithNoId() {
        // Test single resource without ID
        Observation obs = new Observation();
        List<String> resourceIds = FhirUtils.getInputResourceIds(obs);
        assertTrue(resourceIds.isEmpty());

        // Test bundle with resources without IDs
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        bundle.addEntry().setResource(new Observation());
        resourceIds = FhirUtils.getInputResourceIds(bundle);
        assertTrue(resourceIds.isEmpty());
    }

    @Test
    void getInputResourceIdsWithMixedResources() {
        Bundle bundle = new Bundle();
        
        // Add resource with ID
        Patient patient = new Patient();
        patient.setId("test-patient-id");
        bundle.addEntry().setResource(patient);

        // Add resource without ID
        Observation obs = new Observation();
        bundle.addEntry().setResource(obs);

        List<String> resourceIds = FhirUtils.getInputResourceIds(bundle);
        assertEquals(1, resourceIds.size());
        assertEquals("Patient/test-patient-id", resourceIds.get(0));
    }

    @Test
    void getInputResourceIdsWithDuplicateIds() {
        Bundle bundle = new Bundle();
        
        // Add multiple resources with same ID
        Patient patient1 = new Patient();
        patient1.setId("duplicate-id");
        bundle.addEntry().setResource(patient1);

        Patient patient2 = new Patient();
        patient2.setId("duplicate-id");
        bundle.addEntry().setResource(patient2);

        List<String> resourceIds = FhirUtils.getInputResourceIds(bundle);
        assertEquals(1, resourceIds.size());
        assertEquals("Patient/duplicate-id", resourceIds.get(0));
    }

    @Test
    void getInputResourceIdsWithNullResource() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(null);
        List<String> resourceIds = FhirUtils.getInputResourceIds(bundle);
        assertTrue(resourceIds.isEmpty());
    }

    @Test
    void getInputResourceIdsWithInvalidJson() {
        String invalidJson = "{invalid json}";
        
        Exception exception = assertThrows(DataFormatException.class, () -> {
            Resource resource = (Resource) fhirContext.newJsonParser().parseResource(invalidJson);
            FhirUtils.getInputResourceIds(resource);
        });
        assertTrue(exception.getMessage().contains("Failed to parse JSON"));
    }

    @Test
    void getInputResourceIdsWithEmptyResource() {
        Resource emptyResource = new Basic();  // Basic is the simplest FHIR resource type
        List<String> resourceIds = FhirUtils.getInputResourceIds(emptyResource);
        assertTrue(resourceIds.isEmpty());
    }

    @Test
    void extractInputParameters_Bundle_WithValidProfiles() {
        // Create a test Bundle with multiple entries
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Add first entry with meta profile
        Bundle.BundleEntryComponent entry1 = bundle.addEntry();
        Patient patient = new Patient();
        patient.getMeta().addProfile("http://test.profile/Patient");
        entry1.setResource(patient);
        entry1.getRequest().setMethod(Bundle.HTTPVerb.POST);

        // Add second entry with same request method
        Bundle.BundleEntryComponent entry2 = bundle.addEntry();
        Observation obs = new Observation();
        obs.getMeta().addProfile("http://test.profile/Observation");
        entry2.setResource(obs);
        entry2.getRequest().setMethod(Bundle.HTTPVerb.POST);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_OBJECT, bundle);

        // Execute
        FhirUtils.extractInputParameters(exchange);

        // Verify
        assertEquals("POST", exchange.getIn().getHeader(CamelConstants.REQUEST_HTTP_METHOD));
        assertEquals("Bundle", exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE));
        List<String> profiles = exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PROFILE, List.class);
        assertNotNull(profiles);
        assertEquals(2, profiles.size());
        assertTrue(profiles.contains("http://test.profile/Patient"));
        assertTrue(profiles.contains("http://test.profile/Observation"));
    }

    @Test
    void extractInputParameters_SingleResource_WithValidProfile() {
        // Create a test Patient resource
        Patient patient = new Patient();
        patient.getMeta().addProfile("http://test.profile/Patient");

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_OBJECT, patient);
        exchange.setProperty(Exchange.HTTP_METHOD, "POST");

        // Execute
        FhirUtils.extractInputParameters(exchange);

        // Verify
        assertEquals("POST", exchange.getIn().getHeader(CamelConstants.REQUEST_HTTP_METHOD));
        assertEquals("Patient", exchange.getIn().getHeader(CamelConstants.REQUEST_RESOURCE_TYPE));
        List<String> profiles = exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PROFILE, List.class);
        assertNotNull(profiles);
        assertEquals(1, profiles.size());
        assertEquals("http://test.profile/Patient", profiles.get(0));
    }

    @Test
    void extractInputParameters_Bundle_WithInconsistentMethods() {
        // Create a test Bundle with inconsistent methods
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Add entry with POST method
        Bundle.BundleEntryComponent entry1 = bundle.addEntry();
        entry1.setResource(new Patient());
        entry1.getRequest().setMethod(Bundle.HTTPVerb.POST);

        // Add entry with PUT method
        Bundle.BundleEntryComponent entry2 = bundle.addEntry();
        entry2.setResource(new Observation());
        entry2.getRequest().setMethod(Bundle.HTTPVerb.PUT);

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_OBJECT, bundle);

        // Execute and verify exception
        assertThrows(IllegalArgumentException.class, () -> 
            FhirUtils.extractInputParameters(exchange),
            "Should throw exception for inconsistent methods"
        );
    }

    @Test
    void extractInputParameters_WithoutProfile() {
        // Create a test Patient resource without profile
        Patient patient = new Patient();

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_OBJECT, patient);
        exchange.setProperty(Exchange.HTTP_METHOD, "POST");

        // Execute and verify exception
        assertThrows(IllegalArgumentException.class, () -> 
            FhirUtils.extractInputParameters(exchange),
            "Should throw exception for missing profile"
        );
    }

    @Test
    void extractInputParameters_WithMetaSource() {
        // Create a test Patient resource with meta source
        Patient patient = new Patient();
        patient.getMeta().addProfile("http://test.profile/Patient");
        patient.getMeta().setSource("http://test.source");

        // Set up exchange
        exchange.getIn().setHeader(CamelConstants.TEMP_REQUEST_RESOURCE_OBJECT, patient);
        exchange.setProperty(Exchange.HTTP_METHOD, "POST");

        // Execute
        FhirUtils.extractInputParameters(exchange);

        // Verify
        assertEquals("http://test.source", exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_SOURCE));
    }
}

