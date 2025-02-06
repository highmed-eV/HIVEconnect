package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestTemplateEndpoint;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.response.openehr.TemplatesResponseData;
import org.ehrbase.webtemplate.templateprovider.TemplateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultRestTemplateEndpointTest {

    private DefaultRestTemplateEndpoint defaultRestTemplateEndpoint;

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    @Mock
    TemplateProvider templateProvider;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    @BeforeEach
    void setUp() throws IOException {
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/", templateProvider));
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

    @Test
    void findAllTemplates() throws Exception {
        String jsonResponse = "[{\"template_id\":\"template1\",\"archetype_id\":\"archetype1\",\"concept\":\"Concept One\",\"created_timestamp\":\"2023-02-15T10:00:00.000+00:00\"}]";
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)));

        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(any(URI.class), any(), eq(ContentType.APPLICATION_JSON.getMimeType()));

        TemplatesResponseData result = defaultRestTemplateEndpoint.findAllTemplates();

        assertNotNull(result);
        assertNotNull(result.get());
        assertEquals(1, result.get().size());
        TemplateMetaDataDto template = result.get().get(0);
        assertEquals("template1", template.getTemplateId());
        assertEquals("archetype1", template.getArchetypeId());
        assertEquals("Concept One", template.getConcept());
        assertNotNull(template.getCreatedOn());
        verify(testableDefaultRestClient).internalGet(any(URI.class), any(), eq(ContentType.APPLICATION_JSON.getMimeType()));
    }
}

