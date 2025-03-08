/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.fhirbridge.config.openehr;

import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClient;
import org.ehrbase.fhirbridge.openfhir.openfhirclient.OpenFHIRAdapter;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Component;
import org.ehrbase.fhirbridge.openehr.DefaultTemplateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import javax.annotation.PostConstruct;

/**
 * Uploads the operational templates into an openEHR-based CDR.
 *
 * @author Renaud Subiger
 * @since 1.6
 */
@Component
public class OperationalTemplateUploader {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OpenEhrClient openEhrClient;

    private final OpenFHIRAdapter openFHIRAdapter;

    private final DefaultTemplateProvider templateProvider;

    public OperationalTemplateUploader(OpenEhrClient openEhrClient, OpenFHIRAdapter openFHIRAdapter, DefaultTemplateProvider templateProvider) {
        this.openEhrClient = openEhrClient;
        this.openFHIRAdapter = openFHIRAdapter;
        this.templateProvider = templateProvider;
    }

    
    public void uploadTemplates() {
        log.info("Uploading templates...");

        for (var templateId : templateProvider.getTemplateIds()) {
            //check if template exists in cache
            Optional<OPERATIONALTEMPLATE> opt = templateProvider.find(templateId);
            if (opt.isEmpty()) {
                log.info("Template does not exist in Cache: {}", templateId);
                continue;
            }

            //upload template to openEHR
            openEhrClient.templateEndpoint().ensureExistence(templateId);

            //upload template to openFHIR
            openFHIRAdapter.ensureExistence(templateId);
        }
    }
}
