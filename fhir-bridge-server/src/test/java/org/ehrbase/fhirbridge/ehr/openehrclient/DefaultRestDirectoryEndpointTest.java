//package org.ehrbase.fhirbridge.ehr.openehrclient;
//
//import com.nedap.archie.rm.datavalues.DvText;
//import com.nedap.archie.rm.directory.Folder;
//import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestClient;
//import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestDirectoryEndpoint;
//import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
//import org.ehrbase.response.openehr.DirectoryResponseData;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//import java.util.UUID;
//import static org.mockito.Mockito.*;
//import static org.assertj.core.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultRestDirectoryEndpointTest {
//
//    @Mock
//    private DefaultRestClient mockRestClient;
//
//    @Mock
//    private DirectoryResponseData mockDirectoryResponseData;
//
//    @Mock
//    private VersionUid mockVersionUid;
//
//    @Mock
//    private DefaultRestDirectoryEndpoint directoryEndpoint;
//
//    private final UUID ehrId = UUID.randomUUID();
//
//    @BeforeEach
//    void setUp() {
//        directoryEndpoint = new DefaultRestDirectoryEndpoint(mockRestClient, ehrId);
//    }
//
//    @Test
//    void testSyncFromDb_whenRootIsNull_createsRoot() {
//        // Mock the behavior for the root creation and syncing
//        when(mockRestClient.httpGet(any(), eq(DirectoryResponseData.class)))
//                .thenReturn(Optional.of(mockDirectoryResponseData));
//        when(mockRestClient.httpPost(any(), any())).thenReturn(mockVersionUid);
//
//        directoryEndpoint.syncFromDb();
//
//        // Verify that root was created and synced
//        assertThat(directoryEndpoint.getRoot()).isNotNull();
//        verify(mockRestClient, times(1)).httpPost(any(), any());
//    }
//
//    @Test
//    void testSaveToDb_savesRootAndSyncs() {
//        // Mock responses
//        when(mockRestClient.httpPut(any(), any(), any())).thenReturn(mockVersionUid);
//        when(mockRestClient.httpGet(any(), eq(DirectoryResponseData.class)))
//                .thenReturn(Optional.of(mockDirectoryResponseData));
//
//        directoryEndpoint.saveToDb();
//
//        // Verify that the save and sync were called
//        verify(mockRestClient, times(1)).httpPut(any(), any(), any());
//        verify(mockRestClient, times(1)).httpGet(any(), eq(DirectoryResponseData.class));
//    }
//
//    @Test
//    void testFind_whenPathIsEmpty_returnsRoot() {
//        // Mock the behavior for syncing
//        when(mockRestClient.httpGet(any(), eq(DirectoryResponseData.class)))
//                .thenReturn(Optional.of(mockDirectoryResponseData));
//
//        // Ensure the method returns the root if path is empty
//        Folder result = directoryEndpoint.find("");
//
//        assertThat(result).isEqualTo(directoryEndpoint.getRoot());
//    }
//
//    @Test
//    void testFind_whenPathIsNotEmpty_createsFolderStructure() {
//        // Mock the behavior for syncing
//        when(mockRestClient.httpGet(any(), eq(DirectoryResponseData.class)))
//                .thenReturn(Optional.of(mockDirectoryResponseData));
//
//        String path = "folder1/folder2";
//        Folder result = directoryEndpoint.find(path);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getName().getValue()).isEqualTo("folder2");
//    }
//
//    @Test
//    void testGetFolder_returnsFolderDAO() {
//        // Mock the behavior for folder DAO
//        DefaultRestFolderDAO mockFolderDAO = mock(DefaultRestFolderDAO.class);
//        when(mockFolderDAO.sync()).thenReturn(mockFolderDAO);
//
//        FolderDAO folderDAO = directoryEndpoint.getFolder("folderPath");
//
//        assertThat(folderDAO).isInstanceOf(DefaultRestFolderDAO.class);
//        verify(mockFolderDAO).sync();
//    }
//
//    @Test
//    void testResolve_whenSubPathIsEmpty_returnsBaseUri() {
//        URI expectedUri = URI.create("http://baseUri/ehr/1234/directory/");
//        when(mockRestClient.getConfig().getBaseUri()).thenReturn(URI.create("http://baseUri/"));
//
//        URI result = directoryEndpoint.resolve("");
//
//        assertThat(result).isEqualTo(expectedUri);
//    }
//
//    @Test
//    void testResolve_whenSubPathIsNotEmpty_returnsResolvedUri() {
//        URI expectedUri = URI.create("http://baseUri/ehr/1234/directory/subpath");
//        when(mockRestClient.getConfig().getBaseUri()).thenReturn(URI.create("http://baseUri/"));
//
//        URI result = directoryEndpoint.resolve("subpath");
//
//        assertThat(result).isEqualTo(expectedUri);
//    }
//}
//
