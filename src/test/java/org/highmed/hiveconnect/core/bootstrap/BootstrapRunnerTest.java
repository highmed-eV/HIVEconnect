package org.highmed.hiveconnect.core.bootstrap;

import org.highmed.hiveconnect.config.openehr.OperationalTemplateUploader;
import org.highmed.hiveconnect.core.domain.BootstrapEntity;
import org.highmed.hiveconnect.core.repository.BootstrapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BootstrapRunnerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private BootstrapRepository bootstrapRepository;

    @Mock
    private OperationalTemplateUploader operationalTemplateUploader;

    @Mock
    private ApplicationArguments applicationArguments;

    private BootstrapRunner bootstrapRunner;

    @BeforeEach
    void setUp() {
        bootstrapRunner = new BootstrapRunner(cacheManager, bootstrapRepository, operationalTemplateUploader,"test-bootstrap", false);
    }

    @Test
    void testRunWithNonExistentDirectory() throws Exception {
        // Arrange
        File directory = new File("test-bootstrap");
        if (directory.exists()) {
            directory.delete();
        }

        // Act
        bootstrapRunner.run(applicationArguments);

        // Assert
        verify(bootstrapRepository, never()).findAll();
        verify(operationalTemplateUploader, never()).uploadTemplates();
    }

    @Test
    void testRunWithEmptyDirectory() throws Exception {
        // Arrange
        Path tempDir = Files.createTempDirectory("test-bootstrap");
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapDir", tempDir.toString());
        
        // Act
        bootstrapRunner.run(applicationArguments);

        // Assert
        verify(operationalTemplateUploader).uploadTemplates();

        // Cleanup
        Files.delete(tempDir);
    }

    @Test
    void testRunWithExistingOptFiles() throws Exception {
        // Arrange
        Path tempDir = Files.createTempDirectory("test-bootstrap");
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapDir", tempDir.toString());

        // Create test .opt files with minimal valid XML content
        Path optFile1 = tempDir.resolve("test1.opt");
        Path optFile2 = tempDir.resolve("test2.opt");

        String minimalOptContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<archetype>\n<!-- minimal content -->\n</archetype>";

        Files.writeString(optFile1, minimalOptContent);
        Files.writeString(optFile2, minimalOptContent);

        BootstrapEntity entity1 = new BootstrapEntity("test1.opt");
        BootstrapEntity entity2 = new BootstrapEntity("test2.opt");
        when(bootstrapRepository.findByFile("test1.opt")).thenReturn(Optional.of(entity1));
        when(bootstrapRepository.findByFile("test2.opt")).thenReturn(Optional.of(entity2));

        // Act
        bootstrapRunner.run(applicationArguments);

        // Assert
        verify(bootstrapRepository).findByFile("test1.opt");
        verify(operationalTemplateUploader, times(1)).uploadTemplates();

        // Cleanup
        Files.delete(optFile1);
        Files.delete(optFile2);
        Files.delete(tempDir);
    }


    @Test
    void testRunWithRecursiveDirectory() throws Exception {
        // Arrange
        Path tempDir = Files.createTempDirectory("test-bootstrap");
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapDir", tempDir.toString());
        ReflectionTestUtils.setField(bootstrapRunner, "recursivelyOpenDirectories", true);

        // Create nested directory structure
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);
        Path optFile = subDir.resolve("test.opt");
        Files.createFile(optFile);

        BootstrapEntity entity = new BootstrapEntity("test.opt");
        when(bootstrapRepository.findByFile("subdir/test.opt")).thenReturn(Optional.of(entity));
        
        // Act
        bootstrapRunner.run(applicationArguments);

        // Assert
        verify(bootstrapRepository).findByFile("subdir/test.opt");
        verify(operationalTemplateUploader).uploadTemplates();

        // Cleanup
        Files.delete(optFile);
        Files.delete(subDir);
        Files.delete(tempDir);
    }

    
} 