package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestTemplateEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultRestTemplateEndpointTest {

    private DefaultRestTemplateEndpoint defaultRestTemplateEndpoint;

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    @Mock
    private OPERATIONALTEMPLATE operationalTemplate;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    @BeforeEach
    void setUp() {
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        defaultRestTemplateEndpoint = new DefaultRestTemplateEndpoint(testableDefaultRestClient);
    }

    @Test
    void findTemplateSuccess() throws Exception {
        String templateId = "testTemplateId";
        String validXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><template xmlns=\"http://schemas.openehr.org/v1\"><name>Valid Template</name></template>";

        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(validXml.getBytes(StandardCharsets.UTF_8)));

        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(any(URI.class), any(), anyString());

        Optional<OPERATIONALTEMPLATE> result = defaultRestTemplateEndpoint.findTemplate(templateId);

        assertTrue(result.isPresent());
        verify(testableDefaultRestClient).internalGet(any(URI.class), any(), eq(ContentType.APPLICATION_XML.getMimeType()));
    }

    @Test
    void findTemplateNotFound() {
        String templateId = "nonExistentTemplateId";

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(404);

        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(any(URI.class), any(), anyString());

        Optional<OPERATIONALTEMPLATE> result = defaultRestTemplateEndpoint.findTemplate(templateId);

        assertFalse(result.isPresent());
        verify(testableDefaultRestClient).internalGet(any(URI.class), any(), eq(ContentType.APPLICATION_XML.getMimeType()));
    }

//    @Test
//    void testFindAllTemplates() throws Exception {
//        String jsonResponse = "{\"templates\":[{\"template_id\":\"template1\",\"archetype_id\":\"archetype1\",\"concept\":\"Concept One\",\"created_timestamp\":\"2023-02-15T10:00:00.000+00:00\"}]}";
//        HttpEntity httpEntity = mock(HttpEntity.class);
//
//        when(httpResponse.getStatusLine()).thenReturn(statusLine);
//        when(statusLine.getStatusCode()).thenReturn(200);
//        when(httpResponse.getEntity()).thenReturn(httpEntity);
//        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)));
//
//        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(any(URI.class), any(), anyString());
//
//        TemplatesResponseData result = defaultRestTemplateEndpoint.findAllTemplates();
//
//        assertNotNull(result);
//        assertNotNull(result.get());
//        assertEquals(1, result.get().size());
//        TemplateMetaDataDto template = result.get().get(0);
//        assertEquals("template1", template.getTemplateId());
//        assertEquals("archetype1", template.getArchetypeId());
//        assertEquals("Concept One", template.getConcept());
//        assertNotNull(template.getCreatedOn());
//
//        verify(testableDefaultRestClient).internalGet(any(URI.class), any(), eq(ContentType.APPLICATION_JSON.getMimeType()));
//
//    }

//
//    @Test
//    void testEnsureExistenceTemplateNotFound() {
//        // Arrange
//        String templateId = "testTemplateId";
//        when(defaultRestClient.getTemplateProvider().find(templateId)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        ClientException exception = assertThrows(ClientException.class, () -> endpoint.ensureExistence(templateId));
//        assertEquals("Unknown Template with Id testTemplateId", exception.getMessage());
//    }
//
//    @Test
//    void testEnsureExistenceTemplateExists() throws Exception {
//        // Arrange
//        String templateId = "testTemplateId";
//        OPERATIONALTEMPLATE operationalTemplate = mock(OPERATIONALTEMPLATE.class);
//        when(defaultRestClient.getTemplateProvider().find(templateId)).thenReturn(Optional.of(operationalTemplate));
//        when(endpoint.findTemplate(templateId)).thenReturn(Optional.of(operationalTemplate));
//
//        // Act
//        endpoint.ensureExistence(templateId);
//
//        // Assert
//        verify(defaultRestClient.getTemplateProvider()).find(templateId);
//    }
//
//    @Test
//    void testUpload() throws Exception {
//        // Arrange
//        OPERATIONALTEMPLATE operationalTemplate = mock(OPERATIONALTEMPLATE.class);
//        String templateId = "testTemplateId";
//        when(operationalTemplate.getTemplateId().getValue()).thenReturn(templateId);
//        when(defaultRestClient.internalPost(any(), any(), any(), any(), anyString())).thenReturn(httpResponse);
//        when(httpResponse.getFirstHeader(anyString())).thenReturn(null); // Simulating no header case
//
//        // Act
//        String result = invokePrivateUploadMethod(operationalTemplate);
//
//        // Assert
//        assertEquals(templateId, result);
//    }
//
//    // Reflection to invoke private upload method
//    private String invokePrivateUploadMethod(OPERATIONALTEMPLATE operationalTemplate) throws Exception {
//        var uploadMethod = DefaultRestTemplateEndpoint.class.getDeclaredMethod("upload", OPERATIONALTEMPLATE.class);
//        uploadMethod.setAccessible(true);
//        return (String) uploadMethod.invoke(endpoint, operationalTemplate);
//    }


//    @Test
//    void upload_SuccessfulResponse_ReturnsEtag() {
//        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
//        URI uri = baseUri.resolve(DefaultRestTemplateEndpoint.DEFINITION_TEMPLATE_ADL_1_4_PATH);
//
////        URI uri = URI.create("http://localhost:8080/rest/openehr/v1/definition/template/adl1.4/");
////        when(testableDefaultRestClient.getConfig()).thenReturn(new DefaultRestClientConfig(uri));
//        when(testableDefaultRestClient.internalPost(eq(uri), isNull(), anyString(), any(), anyString())).thenReturn(httpResponse);
//        when(httpResponse.getStatusLine()).thenReturn(statusLine);
//        when(statusLine.getStatusCode()).thenReturn(200);
//        when(httpResponse.getFirstHeader(anyString())).thenReturn(new BasicHeader("ETag", "W/\"template-id\""));
//
//        OPERATIONALTEMPLATE operationalTemplate = mock(OPERATIONALTEMPLATE.class);
//        TEMPLATEID mockTemplateId = mock(TEMPLATEID.class);
//        when(mockTemplateId.getValue()).thenReturn("KDS_Diagnose");
//        when(operationalTemplate.getTemplateId()).thenReturn(mockTemplateId);
//
//        String result = templateEndpoint.upload(operationalTemplate);
//
//        assertEquals("template-id", result);
//        verify(testableDefaultRestClient).internalPost(eq(uri), isNull(), anyString(), any(), anyString());
//    }

//    @Test
//    void upload_MissingEtag_ReturnsTemplateId() {
//        URI uri = URI.create("http://localhost:8080/rest/openehr/v1/definition/template/adl1.4/");
//        when(testableDefaultRestClient.getConfig()).thenReturn(new DefaultRestClientConfig(uri));
//        when(testableDefaultRestClient.internalPost(eq(uri), isNull(), anyString(), any(), anyString())).thenReturn(httpResponse);
//        when(httpResponse.getFirstHeader("ETag")).thenReturn(null);
//        when(operationalTemplate.getTemplateId()).thenReturn(new OPERATIONALTEMPLATE.TemplateId("fallback-template-id"));
//
//        String result = templateEndpoint.upload(operationalTemplate);
//
//        assertEquals("fallback-template-id", result);
//    }

//    @Test
//    void upload_ClientException_ThrowsException() {
//        URI uri = URI.create("http://localhost:8080/rest/openehr/v1/definition/template/adl1.4/");
//        when(testableDefaultRestClient.getConfig()).thenReturn(new DefaultRestClientConfig(uri));
//        when(testableDefaultRestClient.internalPost(eq(uri), isNull(), anyString(), any(), anyString())).thenThrow(new ClientException("Error"));
//
//        assertThrows(ClientException.class, () -> templateEndpoint.upload(operationalTemplate));
//    }
}

