package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestCompositionEndpoint;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint.EHR_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension .class)
public class DefaultRestCompositionEndpointTest {

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
    public void mergeCompositionEntityWithValidComposition() {
        Composition composition = mock(Composition.class);
        VersionUid versionUid = mock(VersionUid.class);
        URI baseUri = testableDefaultRestClient.getConfig().getBaseUri();
        URI targetUri = baseUri.resolve(EHR_PATH + ehrId.toString() + DefaultRestCompositionEndpoint.COMPOSITION_PATH);

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
    public void testMergeCompositionEntity_withExistingVersionUid() {
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

//    @Test
//    public void testMergeRaw_withValidComposition() {
//        // Arrange
//        Composition composition = mock(Composition.class);
//        VersionUid versionUid = mock(VersionUid.class);
//
//        when(composition.getUid()).thenReturn(null); // Simulate no UID
//        when(defaultRestClient.httpPost(any(), any(Composition.class))).thenReturn(versionUid);
//
//        // Act
//        VersionUid result = defaultRestCompositionEndpoint.mergeRaw(composition);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(versionUid.toString(), result.toString());
//        verify(defaultRestClient).httpPost(any(), any(Composition.class));
//    }
//
//    @Test
//    public void testFindRaw_withValidId() {
//        // Arrange
//        UUID compositionId = UUID.randomUUID();
//        Composition composition = mock(Composition.class);
//        when(defaultRestClient.httpGet(any(), eq(Composition.class))).thenReturn(Optional.of(composition));
//
//        // Act
//        Optional<Composition> result = defaultRestCompositionEndpoint.findRaw(compositionId);
//
//        // Assert
//        assertTrue(result.isPresent());
//        assertEquals(composition, result.get());
//        verify(defaultRestClient).httpGet(any(), eq(Composition.class));
//    }
//
//    @Test
//    public void testDelete_withValidPrecedingVersionUid() {
//        // Arrange
//        VersionUid versionUid = mock(VersionUid.class);
//
//        doNothing().when(defaultRestClient).internalDelete(any(), any());
//
//        // Act
//        defaultRestCompositionEndpoint.delete(versionUid);
//
//        // Assert
//        verify(defaultRestClient).internalDelete(any(), any());
//    }
//
//    @Test
//    public void testDelete_withNullPrecedingVersionUid_shouldThrowException() {
//        // Act & Assert
//        assertThrows(ClientException.class, () -> {
//            defaultRestCompositionEndpoint.delete(null);
//        });
//    }
}

