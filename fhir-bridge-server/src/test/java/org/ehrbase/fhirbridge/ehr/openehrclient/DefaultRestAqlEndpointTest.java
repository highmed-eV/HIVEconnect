package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.ehrbase.client.aql.field.AqlField;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void executeTest() throws IOException {
        String mockAqlQuery = "SELECT e/ehr_id FROM EHR e";
        String mockResponseJson = "{\"rows\":[[\"123e4567-e89b-12d3-a456-426614174000\"],[\"987e6543-e21b-32d3-b456-426614174111\"]]}";
        when(query.buildAql()).thenReturn(mockAqlQuery);

        AqlField<Object>[] mockFields = new AqlField[1];
        AqlField<Object> mockEhrIdField = mock(AqlField.class);
        when(mockEhrIdField.getValueClass()).thenReturn((Class) String.class);
        mockFields[0] = mockEhrIdField;
        when(query.fields()).thenReturn(mockFields);

        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(DefaultRestAqlEndpoint.AQL_PATH);
        doReturn(httpResponse).when(testableDefaultRestClient).internalPost(eq(targetUri), isNull(), any(), any(), any());
        when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(mockResponseJson.getBytes()));

        List<Record> result = defaultRestAqlEndpoint.execute(query);
        verify(testableDefaultRestClient).internalPost(eq(targetUri), isNull(), any(), any(), any());
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.get(0).value(0));
        assertEquals("987e6543-e21b-32d3-b456-426614174111", result.get(1).value(0));
    }

    @Test
    void executeRawTest() throws IOException {
        String mockAqlQuery = "SELECT e/ehr_id FROM EHR e";
        String mockResponseJson = "{\"rows\":[[\"123e4567-e89b-12d3-a456-426614174000\"],[\"987e6543-e21b-32d3-b456-426614174111\"]]}";
        when(query.buildAql()).thenReturn(mockAqlQuery);

        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(DefaultRestAqlEndpoint.AQL_PATH);
        doReturn(httpResponse).when(testableDefaultRestClient).internalPost(eq(targetUri), any(), any(), any(), any());
        when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(mockResponseJson.getBytes()));

        QueryResponseData responseData = defaultRestAqlEndpoint.executeRaw(query);

        verify(testableDefaultRestClient).internalPost(eq(targetUri), any(), any(), any(), any());
        assertNotNull(responseData);
        assertEquals(2, responseData.getRows().size());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", responseData.getRows().get(0).get(0));
        assertEquals("987e6543-e21b-32d3-b456-426614174111", responseData.getRows().get(1).get(0));

    }
}
