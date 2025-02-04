package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.nedap.archie.rm.ehr.EhrStatus;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestEhrEndpoint;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRestEhrEndpointTest {

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    private DefaultRestEhrEndpoint defaultRestEhrEndpoint;

    private UUID ehrId;

    @Mock
    EhrStatus ehrStatus;

    @BeforeEach
    public void setUp() {
        ehrId = UUID.randomUUID();
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        defaultRestEhrEndpoint = new DefaultRestEhrEndpoint(testableDefaultRestClient);
    }

    @Test
    void createEhr() {
        doReturn(new VersionUid(ehrId.toString())).when(testableDefaultRestClient).httpPost(any(URI.class), eq(null));

        UUID result = defaultRestEhrEndpoint.createEhr();

        assertNotNull(result);
        assertEquals(ehrId, result);
        verify(testableDefaultRestClient).httpPost(any(URI.class), eq(null));
    }

    @Test
    void createEhrWithEhrStatus() {
        doReturn(new VersionUid(ehrId.toString())).when(testableDefaultRestClient).httpPost(any(URI.class), eq(ehrStatus));

        UUID result = defaultRestEhrEndpoint.createEhr(ehrStatus);

        assertNotNull(result);
        assertEquals(ehrId, result);
        verify(testableDefaultRestClient).httpPost(any(URI.class), eq(ehrStatus));
    }

    @Test
    void getEhrStatus() {
        doReturn(Optional.of(ehrStatus)).when(testableDefaultRestClient).httpGet(any(URI.class), eq(EhrStatus.class));

        Optional<EhrStatus> result = defaultRestEhrEndpoint.getEhrStatus(ehrId);

        assertTrue(result.isPresent());
        assertEquals(ehrStatus, result.get());
        verify(testableDefaultRestClient).httpGet(any(URI.class), eq(EhrStatus.class));
    }

    @Test
    void UpdateEhrStatus() {
        when(ehrStatus.getUid()).thenReturn(mock(com.nedap.archie.rm.support.identification.ObjectVersionId.class));
        when(ehrStatus.getUid().getValue()).thenReturn("550e8400-e29b-41d4-a716-446655440000::ehr.system::1");
        doReturn(mock(VersionUid.class)).when(testableDefaultRestClient).httpPut(any(URI.class), eq(ehrStatus), any(VersionUid.class));

        defaultRestEhrEndpoint.updateEhrStatus(ehrId, ehrStatus);
        verify(testableDefaultRestClient).httpPut(any(URI.class), eq(ehrStatus), any(VersionUid.class));
    }
}

