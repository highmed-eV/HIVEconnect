package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestAqlEndpoint;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRestAqlEndpointTest {

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    @Mock
    private Query<Record> query;

    @Mock
    private DefaultRestAqlEndpoint defaultRestAqlEndpoint;

    @BeforeEach
    void setUp() {
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        defaultRestAqlEndpoint = new DefaultRestAqlEndpoint(testableDefaultRestClient);
    }

//    @Test
    void executeTest() throws IOException {
        String mockAqlQuery = "SELECT * FROM Observation";
        String mockResponseJson = "{\"rows\":[[\"2023-01-01\", \"123.45\"]]}";
        when(query.buildAql()).thenReturn(mockAqlQuery);

        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(DefaultRestAqlEndpoint.AQL_PATH);
//        when(testableDefaultRestClient.internalPost(eq(targetUri), isNull(), any(), any(), any())).thenReturn(httpResponse);
        doReturn(httpResponse).when(testableDefaultRestClient).internalPost(eq(targetUri), isNull(), any(), any(), any());
        when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(mockResponseJson.getBytes()));
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);


        List<Record> result = defaultRestAqlEndpoint.execute(query);
        verify(testableDefaultRestClient).internalPost(eq(targetUri), isNull(), any(), any(), any());
        assert result.size() == 1;
        assert result.get(0).value(0).equals("2023-01-01");
        assert result.get(0).value(1).equals("123.45");
    }

//    @Test
    void executeRawTest() throws IOException {
        // Arrange
        String mockAqlQuery = "SELECT * FROM Observation";
        String mockResponseJson = "{\"rows\":[[\"2023-01-01\", \"123.45\"]]}";

        // Mock the HTTP response
        when(testableDefaultRestClient.internalPost(any(), any(), any(), any(), any())).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(mockResponseJson.getBytes()));
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);

        // Act
        QueryResponseData responseData = defaultRestAqlEndpoint.executeRaw(query);

        // Assert
        verify(testableDefaultRestClient).internalPost(any(), any(), any(), any(), any());
        assert responseData != null;
        assert responseData.getRows().size() == 1;
    }
}
