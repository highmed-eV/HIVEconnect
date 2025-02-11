package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.nedap.archie.rm.directory.Folder;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestDirectoryEndpoint;
import org.ehrbase.fhirbridge.openehr.openehrclient.FolderDAO;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.ehrbase.response.openehr.DirectoryResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRestDirectoryEndpointTest {

    @Mock
    private TestableDefaultRestClient testableDefaultRestClient;

    @Mock
    private DefaultRestDirectoryEndpoint defaultRestDirectoryEndpoint;

    private UUID ehrId;

    @BeforeEach
    public void setUp() {
        ehrId = UUID.randomUUID();
        testableDefaultRestClient = spy(new TestableDefaultRestClient("http://localhost:8080/"));
        DirectoryResponseData directoryResponseData = mock(DirectoryResponseData.class);
        doReturn(Optional.of(directoryResponseData)).when(testableDefaultRestClient).httpGet(any(), eq(DirectoryResponseData.class));
        VersionUid versionUid = mock(VersionUid.class);
        doReturn(versionUid).when(testableDefaultRestClient).httpPost(any(), any(Folder.class));
        doReturn(versionUid).when(testableDefaultRestClient).httpPut(any(), any(Folder.class), any(VersionUid.class));
        defaultRestDirectoryEndpoint = new DefaultRestDirectoryEndpoint(testableDefaultRestClient, ehrId);
    }

    @Test
    void getFolderWithExistingPath() {
        FolderDAO folderDAO = defaultRestDirectoryEndpoint.getFolder("existingPath");
        assertNotNull(folderDAO);
    }

    @Test
    void getFolderWithEmptyPathReturnsRoot() {
        FolderDAO folderDAO = defaultRestDirectoryEndpoint.getFolder("");
        assertNotNull(folderDAO);
    }
}
