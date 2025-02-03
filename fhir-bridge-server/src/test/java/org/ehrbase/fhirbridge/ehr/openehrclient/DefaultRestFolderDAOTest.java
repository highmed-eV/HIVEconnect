//package org.ehrbase.fhirbridge.ehr.openehrclient;
//
//import com.nedap.archie.rm.datavalues.DvText;
//import com.nedap.archie.rm.directory.Folder;
//import org.ehrbase.client.aql.query.Query;
//import org.ehrbase.client.exception.ClientException;
//import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestDirectoryEndpoint;
//import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestFolderDAO;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.lang.reflect.Method;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultRestFolderDAOTest {
//
//    @Mock
//    private DefaultRestDirectoryEndpoint directoryEndpoint;
//
//    @InjectMocks
//    private DefaultRestFolderDAO folderDAO;
//
//    @Mock
//    private Folder mockFolder;
//
//    @Mock
//    private DvText dvText;
//
//    private UUID ehrId;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        ehrId = UUID.randomUUID();
//        folderDAO = new DefaultRestFolderDAO(directoryEndpoint, "path/to/folder");
//
//        // Common mock behavior
//        when(dvText.getValue()).thenReturn("Test Folder");
//        when(mockFolder.getName()).thenReturn(dvText);
//
//        when(directoryEndpoint.syncFromDb()).thenReturn(mockFolder);
//    }
//
//    @Test
//    void testGetName() {
//        DvText dvText = mock(DvText.class);
//        when(dvText.getValue()).thenReturn("Test Folder");
//        when(mockFolder.getName()).thenReturn(dvText);
//
//        String folderName = folderDAO.getName();
//
//        assertEquals("Test Folder", folderName);
////        verify(directoryEndpoint, times(1)).syncFromDb();
//    }
//
//    @Test
//    void testSetName() {
//        DvText dvText = mock(DvText.class);
//        when(mockFolder.getName()).thenReturn(dvText);
//        doNothing().when(directoryEndpoint).saveToDb();
//
//        folderDAO.setName("New Folder");
//
//        verify(mockFolder, times(1)).setName(any(DvText.class)); // Verify setName was called
//        verify(directoryEndpoint, times(1)).syncFromDb(); // Ensure syncFromDb was called
//        verify(directoryEndpoint, times(1)).saveToDb(); // Ensure saveToDb was called
//    }
//
//    @Test
//    void testListSubFolderNames() {
//        DvText dvText = mock(DvText.class);
//        when(dvText.getValue()).thenReturn("Sub Folder");
//        Folder subFolder = mock(Folder.class);
//        when(subFolder.getName()).thenReturn(dvText);
//
//        when(mockFolder.getFolders()).thenReturn(List.of(subFolder));
//
//        var subFolderNames = folderDAO.listSubFolderNames();
//
//        assertTrue(subFolderNames.contains("Sub Folder"));
//        verify(directoryEndpoint, times(1)).syncFromDb(); // Ensure syncFromDb was called
//    }
//
//    @Test
//    void testAddCompositionEntity() {
//        // Mock entity and other related behavior
//        Object entity = new Object(); // Replace with actual entity type
//        when(directoryEndpoint.getCompositionEndpoint().mergeCompositionEntity(entity)).thenReturn(entity);
//        UUID uuid = UUID.randomUUID();
//        when(DefaultRestCompositionEndpoint.extractVersionUid(entity)).thenReturn(Optional.of(new ObjectVersionId(uuid.toString())));
//
//        Object updatedEntity = folderDAO.addCompositionEntity(entity);
//
//        assertNotNull(updatedEntity);
//        verify(directoryEndpoint, times(1)).syncFromDb(); // Ensure syncFromDb was called
//        verify(directoryEndpoint, times(1)).saveToDb(); // Ensure saveToDb was called
//    }
//
//    @Test
//    void testFind() {
//        // Mock the query execution
//        Class<?> clazz = Folder.class; // Replace with actual class
//        when(directoryEndpoint.getDefaultRestClient().aqlEndpoint().execute(any(Query.class)))
//                .thenReturn(List.of(mock(Record1.class))); // Replace with actual mock data
//
//        var result = folderDAO.find(clazz);
//
//        assertNotNull(result);
//        verify(directoryEndpoint, times(1)).syncFromDb(); // Ensure syncFromDb was called
//    }
//
//    @Test
//    void testAddCompositionEntityWithoutId() {
//        // Simulate failure case
//        Object entity = new Object();
//        when(directoryEndpoint.getCompositionEndpoint().mergeCompositionEntity(entity)).thenReturn(entity);
//        when(DefaultRestCompositionEndpoint.extractVersionUid(entity)).thenReturn(Optional.empty());
//
//        ClientException exception = assertThrows(ClientException.class, () -> folderDAO.addCompositionEntity(entity));
//        assertEquals("No Id Element for class java.lang.Object", exception.getMessage());
//    }
//}