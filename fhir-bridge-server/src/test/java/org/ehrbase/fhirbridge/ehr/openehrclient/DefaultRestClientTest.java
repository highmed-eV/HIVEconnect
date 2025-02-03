//package org.ehrbase.fhirbridge.ehr.openehrclient;
//
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.nedap.archie.rm.RMObject;
//import com.nedap.archie.rm.archetyped.Locatable;
//import org.apache.http.Header;
//import org.apache.http.StatusLine;
//import org.ehrbase.client.exception.ClientException;
//import org.ehrbase.client.exception.OptimisticLockException;
//import org.ehrbase.client.exception.WrongStatusCodeException;
//import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestClient;
//import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClientConfig;
//import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
//import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
//import org.ehrbase.webtemplate.templateprovider.TemplateProvider;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.fluent.Executor;
//import org.apache.http.client.fluent.Request;
//import org.apache.http.entity.ContentType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import java.lang.reflect.Method;
//import java.net.URI;
//import java.util.Map;
//import java.util.Optional;
//import org.apache.http.impl.client.HttpClients;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultRestClientTest {
//
//    private static final URI TEST_URI = URI.create("http://localhost:8080/");
//
//    private DefaultRestClient client;
//
//    @Mock
//    private OpenEhrClientConfig config;
//
//    @Mock
//    private TemplateProvider templateProvider;
//
//    @Mock
//    private Header header;
//
//    @Mock
//    private HttpResponse httpResponse;
//
////    @Mock
////    RMObject body;
//
//    @Mock
//    StatusLine statusLine;
//
//    @Mock
//    private Executor executor;
//
//    @BeforeEach
//    void setUp() {
//        client = new DefaultRestClient(config, templateProvider, HttpClients.createDefault());
//    }
//
//    @Test
//    void testHttpPost() throws Exception {
//        RMObject body = mock(RMObject.class);
//
//        CanonicalJson canonicalJsonMock = mock(CanonicalJson.class);
//        String expectedBodyString = "{\"mocked\": \"json\"}";
//        when(canonicalJsonMock.marshal(body)).thenReturn(expectedBodyString);
//
////        RMObject body = new RMObject();
////        Method createObjectMapperMethod = DefaultRestClient.class.getDeclaredMethod("createObjectMapper");
////        createObjectMapperMethod.setAccessible(true);
////        ObjectMapper objectMapper = (ObjectMapper) createObjectMapperMethod.invoke(client);
////        String serializedBody = objectMapper.writeValueAsString(body);
//
//        // Use reflection to call protected method httpPost
//        Method method = DefaultRestClient.class.getDeclaredMethod("httpPost", URI.class, RMObject.class);
//        method.setAccessible(true);
//
//        when(httpResponse.getFirstHeader("ETag")).thenReturn(header);
//        when(httpResponse.getStatusLine()).thenReturn(statusLine);
//        when(statusLine.getStatusCode()).thenReturn(200);
//
//        // Call method and assert output
//        VersionUid result = (VersionUid) method.invoke(client, TEST_URI, body);
//        assertNotNull(result);
//    }
//
//    @Test
//    void testHttpPut() throws Exception {
//        URI uri = URI.create("http://localhost:8080/");
//        Locatable body = mock(Locatable.class);
//        VersionUid versionUid = new VersionUid("v1");
//
//        Method method = DefaultRestClient.class.getDeclaredMethod("httpPut", URI.class, Locatable.class, VersionUid.class);
//        method.setAccessible(true);
//
//        HttpResponse mockResponse = mock(HttpResponse.class);
//        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(200);
//        when(mockResponse.getFirstHeader("ETag")).thenReturn(mock(Header.class));
//
//        VersionUid result = (VersionUid) method.invoke(client, uri, body, versionUid);
//        assertNotNull(result);
//    }
//
//    @Test
//    void testHttpGet() throws Exception {
//        URI uri = URI.create("http://localhost:8080/");
//        Class<String> valueType = String.class;
//
//        Method method = DefaultRestClient.class.getDeclaredMethod("httpGet", URI.class, Class.class);
//        method.setAccessible(true);
//
//        HttpResponse mockResponse = mock(HttpResponse.class);
//        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(200);
//        when(mockResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));
//
//        Optional<String> result = (Optional<String>) method.invoke(client, uri, valueType);
//        assertTrue(result.isPresent());
//    }
//
//    @Test
//    void testHttpDelete() throws Exception {
//        URI uri = URI.create("http://localhost:8080/");
//
//        Method method = DefaultRestClient.class.getDeclaredMethod("internalDelete", URI.class, Map.class);
//        method.setAccessible(true);
//
//        HttpResponse mockResponse = mock(HttpResponse.class);
//        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(200);
//
//        HttpResponse result = (HttpResponse) method.invoke(client, uri, null);
//        assertNotNull(result);
//    }
//
//    @Test
//    void testCheckStatus() throws Exception {
//        HttpResponse mockResponse = mock(HttpResponse.class);
//        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(200);
//
//        Method method = DefaultRestClient.class.getDeclaredMethod("checkStatus", HttpResponse.class, int[].class);
//        method.setAccessible(true);
//
//        // This should not throw an exception
//        method.invoke(client, mockResponse, new int[]{200, 201});
//    }
//
////    @Test
////    void testCheckStatusException() {
////        HttpResponse mockResponse = mock(HttpResponse.class);
////        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(404);
////
////        Method method = DefaultRestClient.class.getDeclaredMethod("checkStatus", HttpResponse.class, int[].class);
////        method.setAccessible(true);
////
////        assertThrows(WrongStatusCodeException.class, () -> {
////            method.invoke(client, mockResponse, new int[]{200, 201});
////        });
////    }
//}
//
