package org.highmed.hiveconnect.core.bootstrap;

import org.highmed.hiveconnect.config.openehr.OperationalTemplateUploader;
import org.highmed.hiveconnect.core.domain.BootstrapEntity;
import org.highmed.hiveconnect.core.repository.BootstrapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

class BootstrapRunnerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private BootstrapRepository bootstrapRepository;

    @Mock
    private OperationalTemplateUploader operationalTemplateUploader;

    private BootstrapRunner bootstrapRunner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Manuelle Instanziierung mit gemockten Abhängigkeiten
        bootstrapRunner = new BootstrapRunner(
            cacheManager,
            bootstrapRepository,
            operationalTemplateUploader,
            "/some/test/path", // kann überschrieben werden
            false
        );

        when(cacheManager.getCache("templateCache")).thenReturn(cache);
    }


    @Test
    void testProcessOptFile_NewFile() throws Exception {
        Path tempDir = Files.createTempDirectory("bootstrap-test");
        File tempFile = new File(tempDir.toFile(), "test.opt");

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("""
            <template xmlns="http://schemas.openehr.org/v1">
                <template_id>
                    <value>test-template</value>
                </template_id>
            </template>
        """.getBytes());
        }

        when(bootstrapRepository.findByFile(anyString())).thenReturn(Optional.empty());

        bootstrapRunner = new BootstrapRunner(
            cacheManager,
            bootstrapRepository,
            operationalTemplateUploader,
            tempDir.toString(),
            false
        );

        bootstrapRunner.processOptFile(tempFile.toPath());

        verify(cache).put(eq("test-template"), any());
        verify(bootstrapRepository, times(1)).save(any(BootstrapEntity.class));
    }


    @Test
    void testProcessOptFile_ExistingFile() throws Exception {
        File tempFile = File.createTempFile("test", ".opt");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("""
                <template xmlns="http://schemas.openehr.org/v1">
                    <template_id>
                        <value>existing-template</value>
                    </template_id>
                </template>
            """.getBytes());
        }

        Path path = tempFile.toPath();
        BootstrapEntity existingEntity = new BootstrapEntity("existing-template.opt");
        existingEntity.setUpdatedDateTime(LocalDateTime.now().minusDays(1));

        when(bootstrapRepository.findByFile(anyString())).thenReturn(Optional.of(existingEntity));

        bootstrapRunner = new BootstrapRunner(cacheManager, bootstrapRepository, operationalTemplateUploader,
            tempFile.getParent(), false);

        bootstrapRunner.run(null);

        verify(cache).put(eq("existing-template"), any());
        verify(bootstrapRepository).save(existingEntity);
        verify(operationalTemplateUploader).uploadTemplates();

        tempFile.deleteOnExit();
    }
}
