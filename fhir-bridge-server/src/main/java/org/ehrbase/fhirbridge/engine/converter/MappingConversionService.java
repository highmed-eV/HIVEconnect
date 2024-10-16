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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nedap.archie.rm.composition.Composition;
import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.fhir.common.Profile;
import org.ehrbase.fhirbridge.fhir.support.Resources;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MappingConversionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public Object convert(Exchange exchange) {

        Resource resource = exchange.getIn().getBody(Resource.class);
        Optional<Profile> profileOpt = (Optional<Profile>) exchange.getIn().getHeader(CamelConstants.PROFILE);

        String profileUri = null;
        if (profileOpt.isPresent()) {
            profileUri = profileOpt.get().getUri();
            log.info("The URI for the profile is: " + profileUri);
        } else {
            throw new IllegalArgumentException("Profile not found for resource: " + resource.getResourceType());
        }

        String canonicalJsonFile = ProfileToCanonicalMapper.getProfileToCanonicalMap(profileUri);
        log.info("Profile to be mapped: " + profileUri + "canaocal json: " + canonicalJsonFile);
        if (canonicalJsonFile == null) {
            throw new IllegalArgumentException("Unsupported resource type: " + resource.getResourceType());
        }

        if(Resources.isPatient(resource)){
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


        Resource inputResource = (Resource) exchange.getIn().getHeader("CamelFhirBridgeIncomingResource");
        System.out.println(inputResource.toString());
        System.out.println(String.valueOf(inputResource));
        if(Objects.isNull(inputResource)){
            inputResource = resource;
        }

        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "http://localhost:8090/openfhir/toopenehr?flat=true";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Resource> requestEntity = new HttpEntity<>(inputResource, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        System.out.println("Response code: " + response.getStatusCodeValue());
        System.out.println("Response body: " + response.getBody());

//        FhirContext fhirContext = FhirContext.forR4();
//        IParser jsonParser = fhirContext.newJsonParser();
//        jsonParser.setPrettyPrint(false);
//        jsonParser.setSuppressNarratives(true);
//        String resourceJson = jsonParser.encodeResourceToString(inputResource);

//        ObjectMapper objectMapper = new ObjectMapper();
//        String resourceJson;
//        try {
//            resourceJson = objectMapper.writeValueAsString(inputResource);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to convert Resource to JSON", e);
//        }

//        Gson gson = new Gson();
//        String resourceJson = gson.toJson(inputResource);

//        String apiUrl = "http://localhost:8090/openfhir/toopenehr?flat=true";
//        HttpClient httpClient = HttpClient.newHttpClient();
//        System.out.println(resource.toString());
//        System.out.println(resourceJson);
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(apiUrl))
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(String.valueOf(inputResource)))
//                .build();
//        HttpResponse<String> response;
//        try {
//            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException("Failed to call the API to fetch canonical JSON", e);
//        }

        if (response.getStatusCodeValue() != 200) {
            throw new RuntimeException("API returned error response: " + response.getStatusCode() + " - " + response.getBody());
        }

        String canonicalComposition = response.getBody();
        if (canonicalComposition == null || canonicalComposition.isEmpty()) {
            throw new RuntimeException("Empty canonical JSON received from API");
        }

        CanonicalJson canonicalJson = new CanonicalJson();
        Composition composition = canonicalJson.unmarshal(canonicalComposition, Composition.class);

        return composition;

    }

}
