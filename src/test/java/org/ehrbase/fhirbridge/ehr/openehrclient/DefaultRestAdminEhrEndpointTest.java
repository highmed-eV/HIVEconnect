package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestAdminEhrEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRestAdminEhrEndpointTest {

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private DefaultRestAdminEhrEndpoint defaultRestAdminEhrEndpoint;

    @BeforeEach
    void setUp() {
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        defaultRestAdminEhrEndpoint = new DefaultRestAdminEhrEndpoint(testableDefaultRestClient);
    }

    @Test
    void deleteEhrIdNotNullTest() {
        UUID ehrId = UUID.randomUUID();
        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(DefaultRestAdminEhrEndpoint.ADMIN_EHR_PATH + ehrId);

        doReturn(httpResponse).when(testableDefaultRestClient).internalDelete(eq(targetUri), isNull());
        StatusLine statusLine = mock(StatusLine.class);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);

        int statusCode = defaultRestAdminEhrEndpoint.delete(ehrId);

        verify(testableDefaultRestClient).internalDelete(eq(targetUri), isNull());
        verify(httpResponse).getStatusLine();
        assert statusCode == 200;
    }
}
