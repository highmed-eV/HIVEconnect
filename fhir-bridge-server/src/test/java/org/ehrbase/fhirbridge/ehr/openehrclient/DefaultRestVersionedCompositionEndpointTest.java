package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestVersionedCompositionEndpoint;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.ehrbase.response.openehr.RevisionHistoryResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint.EHR_PATH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRestVersionedCompositionEndpointTest {

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    private DefaultRestVersionedCompositionEndpoint defaultRestVersionedCompositionEndpoint;

    private UUID ehrId;

    private UUID versionedObjectUid;

    private URI baseUri;

    @Mock
    private HttpResponse httpResponse;

    @BeforeEach
    void setUp() {
        ehrId = UUID.randomUUID();
        versionedObjectUid = UUID.randomUUID();
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        defaultRestVersionedCompositionEndpoint = new DefaultRestVersionedCompositionEndpoint(testableDefaultRestClient, ehrId);
        baseUri = testableDefaultRestClient.getConfig().getBaseUri();
    }

    @Test
    void findReturnVersionedCompositionWhenExists() {
        VersionedComposition versionedComposition = mock(VersionedComposition.class);
        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid);
        doReturn(Optional.of(versionedComposition)).when(testableDefaultRestClient).httpGet(uri, VersionedComposition.class);

        Optional<VersionedComposition> result = defaultRestVersionedCompositionEndpoint.find(versionedObjectUid);

        assertTrue(result.isPresent());
        assertEquals(versionedComposition, result.get());
    }

    @Test
    void findReturnEmptyWhenNotExists() {
        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid);
        doReturn(Optional.empty()).when(testableDefaultRestClient).httpGet(uri, VersionedComposition.class);

        Optional<VersionedComposition> result = defaultRestVersionedCompositionEndpoint.find(versionedObjectUid);

        assertFalse(result.isPresent());
    }

    @Test
    void findRevisionHistoryReturnWhenExists() {
        RevisionHistoryResponseData responseData = mock(RevisionHistoryResponseData.class);
        List<RevisionHistoryItem> revisionHistory = Collections.singletonList(mock(RevisionHistoryItem.class));
        when(responseData.getRevisionHistoryItems()).thenReturn(revisionHistory);

        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid + DefaultRestVersionedCompositionEndpoint.REVISION_HISTORY_PATH);
        doReturn(Optional.of(responseData)).when(testableDefaultRestClient).httpGet(uri, RevisionHistoryResponseData.class);

        List<RevisionHistoryItem> result = defaultRestVersionedCompositionEndpoint.findRevisionHistory(versionedObjectUid);

        assertEquals(1, result.size());
    }

    @Test
    void findRevisionHistoryWhenNotExists() {
        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid + DefaultRestVersionedCompositionEndpoint.REVISION_HISTORY_PATH);
        doReturn(Optional.empty()).when(testableDefaultRestClient).httpGet(uri, RevisionHistoryResponseData.class);
        List<RevisionHistoryItem> result = defaultRestVersionedCompositionEndpoint.findRevisionHistory(versionedObjectUid);

        assertTrue(result.isEmpty());
    }

    @Test
    void findVersionByIdWhenExists() throws IOException {
        VersionUid versionUid = mock(VersionUid.class);
        when(versionUid.toString()).thenReturn("31f0fe48-db76-4706-8e2f-871eae3d1501::1");

        String validJson = "{\"data\":{\"_type\":\"ORIGINAL_VERSION\",\"uid\":{\"_type\":\"OBJECT_VERSION_ID\",\"value\":\"31f0fe48-db76-4706-8e2f-871eae3d1501::1\"},\"data\":{\"_type\":\"COMPOSITION\",\"archetype_node_id\":\"openEHR-EHR-COMPOSITION.encounter.v1\"}}}";

        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid + DefaultRestVersionedCompositionEndpoint.VERSION_PATH + "/" + versionUid.toString());

        HttpEntity mockEntity = mock(HttpEntity.class);
        when(httpResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(validJson.getBytes(StandardCharsets.UTF_8)));

        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(eq(uri), any(), eq(ContentType.APPLICATION_JSON.getMimeType()));

        Optional<OriginalVersion<Composition>> result = defaultRestVersionedCompositionEndpoint.findVersionById(versionedObjectUid, versionUid, Composition.class);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getData());
        assertTrue(result.get().getData() instanceof Composition);
    }

    @Test
    void findVersionByIdWhenNotExists() {
        VersionUid versionUid = mock(VersionUid.class);
        when(versionUid.toString()).thenReturn("31f0fe48-db76-4706-8e2f-871eae3d1501::1");

        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid + DefaultRestVersionedCompositionEndpoint.VERSION_PATH + "/" + versionUid.toString());

        when(httpResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(eq(uri), any(), eq(ContentType.APPLICATION_JSON.getMimeType()));

        Optional<OriginalVersion<Composition>> result = defaultRestVersionedCompositionEndpoint.findVersionById(versionedObjectUid, versionUid, Composition.class);

        assertFalse(result.isPresent());
    }

    @Test
    void findVersionAtTimeWhenExists() throws Exception {
        LocalDateTime versionAtTime = LocalDateTime.now().minusDays(1);

        String validJson = "{\"data\":{\"_type\":\"ORIGINAL_VERSION\",\"uid\":{\"_type\":\"OBJECT_VERSION_ID\",\"value\":\"31f0fe48-db76-4706-8e2f-871eae3d1501::1\"},\"data\":{\"_type\":\"COMPOSITION\",\"archetype_node_id\":\"openEHR-EHR-COMPOSITION.encounter.v1\"}}}";

        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid + DefaultRestVersionedCompositionEndpoint.VERSION_PATH);
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.addParameter("version_at_time", versionAtTime.format(DateTimeFormatter.ISO_DATE_TIME));

        HttpEntity mockEntity = mock(HttpEntity.class);
        when(httpResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(validJson.getBytes(StandardCharsets.UTF_8)));

        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(eq(uriBuilder.build()), any(), eq(ContentType.APPLICATION_JSON.getMimeType()));

        Optional<OriginalVersion<Composition>> result = defaultRestVersionedCompositionEndpoint.findVersionAtTime(versionedObjectUid, versionAtTime, Composition.class);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getData());
        assertTrue(result.get().getData() instanceof Composition);
    }

    @Test
    void findVersionAtTimeWhenNotExists() throws Exception {
        LocalDateTime versionAtTime = LocalDateTime.now().minusDays(1);

        URI uri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestVersionedCompositionEndpoint.VERSIONED_COMPOSITION_PATH + versionedObjectUid + DefaultRestVersionedCompositionEndpoint.VERSION_PATH);
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.addParameter("version_at_time", versionAtTime.format(DateTimeFormatter.ISO_DATE_TIME));

        when(httpResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        doReturn(httpResponse).when(testableDefaultRestClient).internalGet(eq(uriBuilder.build()), any(), eq(ContentType.APPLICATION_JSON.getMimeType()));

        Optional<OriginalVersion<Composition>> result = defaultRestVersionedCompositionEndpoint.findVersionAtTime(versionedObjectUid, versionAtTime, Composition.class);

        assertFalse(result.isPresent());
    }

}

