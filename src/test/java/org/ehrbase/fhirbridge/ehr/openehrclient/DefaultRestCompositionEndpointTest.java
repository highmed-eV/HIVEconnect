package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestCompositionEndpoint;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint.EHR_PATH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension .class)
class DefaultRestCompositionEndpointTest {

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    private DefaultRestCompositionEndpoint defaultRestCompositionEndpoint;
    private UUID ehrId;

    @BeforeEach
    public void setUp() {
        ehrId = UUID.randomUUID();
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        defaultRestCompositionEndpoint = new DefaultRestCompositionEndpoint(testableDefaultRestClient, ehrId);
    }

    @Test
    void mergeCompositionEntityWithValidComposition() {
        Composition composition = mock(Composition.class);
        VersionUid versionUid = mock(VersionUid.class);
        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestCompositionEndpoint.COMPOSITION_PATH);

        when(composition.getUid()).thenReturn(null);
        doAnswer(invocation -> {
            UIDBasedId uid = invocation.getArgument(0);
            when(composition.getUid()).thenReturn(uid);
            return null;
        }).when(composition).setUid(any(UIDBasedId.class));
        doReturn(versionUid).when(testableDefaultRestClient).httpPost(eq(targetUri), any(Composition.class));

        Composition result = defaultRestCompositionEndpoint.mergeCompositionEntity(composition);
        assertNotNull(result);
        assertEquals(versionUid.toString(), result.getUid().getValue());
        verify(testableDefaultRestClient).httpPost(any(), any(Composition.class));
    }

    @Test
    void mergeCompositionEntityWithExistingVersionUid() {
        Composition composition = mock(Composition.class);
        VersionUid existingVersionUid = mock(VersionUid.class);
        VersionUid updatedVersionUid = mock(VersionUid.class);

        String validExistingVersionUid = "550e8400-e29b-41d4-a716-446655440000::ehr.system::1";
        String validUpdatedVersionUid = "123e4567-e89b-12d3-a456-426614174000::ehr.system::2";
        when(updatedVersionUid.toString()).thenReturn(validUpdatedVersionUid);

        when(existingVersionUid.getUuid()).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        ObjectVersionId objectVersionId = new ObjectVersionId(validExistingVersionUid);
        when(composition.getUid()).thenReturn(objectVersionId);

        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestCompositionEndpoint.COMPOSITION_PATH + existingVersionUid.getUuid());

        // Mock static method extractVersionUid
        try (MockedStatic<DefaultRestCompositionEndpoint> mockedStatic = mockStatic(DefaultRestCompositionEndpoint.class)) {
            mockedStatic.when(() -> DefaultRestCompositionEndpoint.extractVersionUid(composition))
                    .thenReturn(Optional.of(existingVersionUid));

            doReturn(updatedVersionUid).when(testableDefaultRestClient)
                    .httpPut(eq(targetUri), any(Composition.class), eq(existingVersionUid));

            Composition result = defaultRestCompositionEndpoint.mergeCompositionEntity(composition);
            assertNotNull(result);
            assertEquals(validUpdatedVersionUid, result.getUid().getValue());
            verify(testableDefaultRestClient).httpPut(eq(targetUri), any(Composition.class), eq(existingVersionUid));
        }
    }

    @Test
    void mergeRawWithValidComposition() {
        Composition composition = mock(Composition.class);
        VersionUid versionUid = mock(VersionUid.class);
        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestCompositionEndpoint.COMPOSITION_PATH);

        when(composition.getUid()).thenReturn(null);
        doReturn(versionUid).when(testableDefaultRestClient).httpPost(eq(targetUri), any(Composition.class));

        VersionUid result = defaultRestCompositionEndpoint.mergeRaw(composition);
        assertNotNull(result);
        assertEquals(versionUid.toString(), result.toString());
        verify(testableDefaultRestClient).httpPost(any(), any(Composition.class));
    }

    @Test
    void findRawWithValidId() {
        Composition composition = mock(Composition.class);
        UUID compositionId = UUID.randomUUID();
        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestCompositionEndpoint.COMPOSITION_PATH +  compositionId);

        doReturn(Optional.of(composition)).when(testableDefaultRestClient).httpGet(targetUri, Composition.class);

        Optional<Composition> result = defaultRestCompositionEndpoint.findRaw(compositionId);
        assertTrue(result.isPresent());
        assertEquals(composition, result.get());
        verify(testableDefaultRestClient).httpGet(targetUri, Composition.class);
    }

    @Test
    void deleteWithValidPrecedingVersionUid() {
        VersionUid versionUid = mock(VersionUid.class);
        when(versionUid.toString()).thenReturn("550e8400-e29b-41d4-a716-446655440000::ehr.system::1");

        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI expectedUri = baseUri.resolve(EHR_PATH + ehrId + DefaultRestCompositionEndpoint.COMPOSITION_PATH + "550e8400-e29b-41d4-a716-446655440000::ehr.system::1");

        doReturn(null).when(testableDefaultRestClient).internalDelete(eq(expectedUri), any());

        defaultRestCompositionEndpoint.delete(versionUid);
        verify(testableDefaultRestClient).internalDelete(expectedUri, new HashMap<>());
    }

    @Test
    void testDeleteWithNullPrecedingVersionUidShouldThrowException() {
        assertThrows(ClientException.class, () -> {
            defaultRestCompositionEndpoint.delete(null);
        });
    }
}