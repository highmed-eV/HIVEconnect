package org.highmed.hiveconnect.core.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.apache.xmlbeans.XmlException;
import org.highmed.hiveconnect.config.openehr.OperationalTemplateUploader;
import org.highmed.hiveconnect.core.domain.BootstrapEntity;
import org.highmed.hiveconnect.core.repository.BootstrapRepository;
import org.openehr.schemas.v1.TemplateDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Component
@Slf4j
public class BootstrapRunner implements ApplicationRunner {

    private final String bootstrapDir;
    private final boolean recursivelyOpenDirectories;
    private final CacheManager cacheManager;
    private final BootstrapRepository bootstrapRepository;
    private final OperationalTemplateUploader operationalTemplateUploader;

    public BootstrapRunner(CacheManager cacheManager, BootstrapRepository bootstrapRepository, OperationalTemplateUploader operationalTemplateUploader
                            ,@Value("${hive-connect.bootstrap.dir:/hive-connect-app/bootstrap/}") String bootstrapDir
                            , @Value("${hive-connect.bootstrap.recursively-open-directories:false}") boolean recursivelyOpenDirectories) {
        this.cacheManager = cacheManager;
        this.bootstrapRepository = bootstrapRepository;
        this.operationalTemplateUploader = operationalTemplateUploader;
        this.bootstrapDir = bootstrapDir;
        this.recursivelyOpenDirectories = recursivelyOpenDirectories;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        File directory = new File(bootstrapDir);
        if (!directory.exists()) {
            log.error("Bootstrap directory for opt files does not exists at: {}", directory.getAbsolutePath());
            return;
        } else{
            log.info("Bootstrap directory for opt files {} ", directory.getAbsolutePath());
        }

        try (Stream<Path> paths = recursivelyOpenDirectories ? 
                Files.walk(directory.toPath()) : 
                Files.list(directory.toPath())) {
            
            paths.filter(path -> path.toString().toLowerCase().endsWith(".opt"))
                .forEach(this::processOptFile);
        }

        // Upload templates to OpenEHR and openFHIR
        operationalTemplateUploader.uploadTemplates();

        log.info("Bootstrap to Upload templates completed...");

    }

    void processOptFile(Path path) {
        File file = path.toFile();
        String relativePath = new File(bootstrapDir).toURI().relativize(file.toURI()).getPath();

        try {
            var bootstrapEntity = bootstrapRepository.findByFile(relativePath);

            log.info("Loading template from file: {}", relativePath);
            var templateDocument = TemplateDocument.Factory.parse(new FileInputStream(file));
            var template = templateDocument.getTemplate();
            
            var templateCache = cacheManager.getCache("templateCache");
            templateCache.put(template.getTemplateId().getValue(), template);


            // Create new bootstrap record if file was modified
            if (bootstrapEntity.isEmpty()) {
                BootstrapEntity bootstrapEntity1 = new BootstrapEntity(relativePath);
                LocalDateTime dateTime = LocalDateTime.now();
                bootstrapEntity1.setCreatedDateTime(dateTime);
                bootstrapEntity1.setUpdatedDateTime(dateTime);

                bootstrapRepository.save(bootstrapEntity1);
            } else {
                // Update existing record - @LastModifiedDate will be updated automatically
                bootstrapEntity.get().setUpdatedDateTime(LocalDateTime.now());
                bootstrapRepository.save(bootstrapEntity.get());
            }

        } catch (IOException | XmlException e) {
            log.error("Failed to process template file: {}",relativePath, e);
        }
    }
} 