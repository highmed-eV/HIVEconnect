/*
 * Copyright 2020-2021 the original author or authors.
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

package org.ehrbase.fhirbridge.engine.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.hl7.fhir.r4.model.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MappingConversionService {

    public Object convert(Resource resource) {

        String resourceId = resource.getId();
        String resourceTypeName = resource.getResourceType().name();
        
        String resourceKey = (resourceTypeName == "BUNDLE")?resourceId:resourceTypeName;
        String canonicalJsonFile = ResourceToCanonicalMapper.getResourceToCanonicalMap(resourceKey);
        if (canonicalJsonFile == null) {
           throw new IllegalArgumentException("Unsupported resource type: " + resource.getResourceType());
       }

        String canonicalJsonFilePath = "canonical_json/" + canonicalJsonFile;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(canonicalJsonFilePath);
        if (inputStream == null) {
            throw new RuntimeException("File not found: " + canonicalJsonFile);
        }

        String canonicalComposition;
        try {
            canonicalComposition = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CanonicalJson canonicalJson = new CanonicalJson();
        Composition composition = canonicalJson.unmarshal(canonicalComposition, Composition.class);

        return composition;
    }


}
