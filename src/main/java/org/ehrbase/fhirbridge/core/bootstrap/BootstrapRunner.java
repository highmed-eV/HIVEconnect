package org.ehrbase.fhirbridge.core.bootstrap;

import org.apache.xmlbeans.XmlException;
import org.ehrbase.fhirbridge.config.openehr.OperationalTemplateUploader;
import org.ehrbase.fhirbridge.core.domain.BootstrapEntity;
import org.ehrbase.fhirbridge.core.repository.BootstrapRepository;
import org.openehr.schemas.v1.TemplateDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class BootstrapRunner implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${fhir-bridge.bootstrap.dir:/fhir-bridge-app/bootstrap/}")
    private String bootstrapDir;

    @Value("${fhir-bridge.bootstrap.recursively-open-directories:false}")
    private boolean recursivelyOpenDirectories;

    private final CacheManager cacheManager;
    private final BootstrapRepository bootstrapRepository;
    private final OperationalTemplateUploader operationalTemplateUploader;

    public BootstrapRunner(CacheManager cacheManager, BootstrapRepository bootstrapRepository, OperationalTemplateUploader operationalTemplateUploader) {
        this.cacheManager = cacheManager;
        this.bootstrapRepository = bootstrapRepository;
        this.operationalTemplateUploader = operationalTemplateUploader;
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

    private void processOptFile(Path path) {
        File file = path.toFile();
        String relativePath = new File(bootstrapDir).toURI().relativize(file.toURI()).getPath();

        try {
            var bootstrapEntity = bootstrapRepository.findByFile(relativePath);
            
            // Check if file was modified since last processing
            // if (bootstrapEntity.isPresent() && 
            //     !Files.getLastModifiedTime(path).toInstant()
            //         .isAfter(bootstrapEntity.get().getUpdatedDateTime().toInstant())) {
            //     log.debug("Skipping unchanged template file: {}", relativePath);
            //     return;
            // }

            log.info("Loading template from file: {}", relativePath);
            var templateDocument = TemplateDocument.Factory.parse(new FileInputStream(file));
            var template = templateDocument.getTemplate();
            
            var templateCache = cacheManager.getCache("templateCache");
            if (templateCache != null) {
                templateCache.put(template.getTemplateId().getValue(), template);
            } else {
                log.error("Template cache not found");
            }
            

            // Create new bootstrap record if file was modified
            if (!bootstrapEntity.isPresent()) {
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
            log.error("Failed to process template file: " + relativePath, e);
        }
    }
} 