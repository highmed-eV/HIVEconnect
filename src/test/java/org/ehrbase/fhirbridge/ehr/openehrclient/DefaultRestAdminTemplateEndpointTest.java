package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestAdminTemplateEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRestAdminTemplateEndpointTest {

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private DefaultRestAdminTemplateEndpoint defaultRestAdminTemplateEndpoint;

    @BeforeEach
    void setUp() {
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        defaultRestAdminTemplateEndpoint = new DefaultRestAdminTemplateEndpoint(testableDefaultRestClient);
    }

    @Test
    void deleteTemplateIdNotNullTest() {
        String templateId = "some-template-id";
        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(DefaultRestAdminTemplateEndpoint.ADMIN_TEMPLATE_PATH + templateId);

        doReturn(httpResponse).when(testableDefaultRestClient).internalDelete(eq(targetUri), isNull());
        StatusLine statusLine = mock(StatusLine.class);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);

        int statusCode = defaultRestAdminTemplateEndpoint.delete(templateId);

        verify(testableDefaultRestClient).internalDelete(eq(targetUri), isNull());
        verify(httpResponse).getStatusLine();
        assert statusCode == 200;
    }
}

