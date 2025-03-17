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

 package org.ehrbase.fhirbridge.openehr.camel;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.apache.camel.Exchange;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record1;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.ehrbase.fhirbridge.exception.ConversionException;
import org.ehrbase.fhirbridge.fhir.support.PatientUtils;
import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClient;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link org.apache.camel.Processor Processor} that retrieves the EHR ID of the patient involved in the current
 * exchange. If the patient does not have an EHR, a new one is created and the EHR ID is stored in the database.
 *
 * @since 1.2.0
 */
@Component(EhrLookupProcessor.BEAN_ID)
@SuppressWarnings("java:S6212")
public class EhrLookupProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "ehrLookupProcessor";

    private static final Logger LOG = LoggerFactory.getLogger(EhrLookupProcessor.class);

    private static final String ARCHETYPE_NODE_ID = "openEHR-EHR-EHR_STATUS.generic.v1";

    private final PatientEhrRepository patientEhrRepository;

    private final OpenEhrClient openEhrClient;

    public EhrLookupProcessor(PatientEhrRepository patientEhrRepository,
                              OpenEhrClient openEhrClient) {
        this.patientEhrRepository = patientEhrRepository;
        this.openEhrClient = openEhrClient;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Patient resource = (Patient) exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_RESOURCE);

        String systemId = (String) exchange.getIn().getHeader(CamelConstants.REQUEST_REMOTE_SYSTEM_ID);
        String patientId = (String) exchange.getIn().getHeader(CamelConstants.FHIR_INPUT_PATIENT_ID);
        String serverPatientIdStr = (String) exchange.getIn().getHeader(CamelConstants.FHIR_SERVER_PATIENT_ID);
        String serverPatientId = extractPatientId(serverPatientIdStr);
        UUID ehrId = Optional.ofNullable(patientEhrRepository.findByInternalPatientIdAndSystemId(serverPatientIdStr, systemId))  
                    .map(PatientEhr::getEhrId) 
                    .orElseGet(() -> createOrGetPatientEhr(resource, patientId, serverPatientId, systemId));
        
        LOG.info("EhrLookupProcessor PatientId to EHRId mapping: PatientId: {} EHRId: {}", patientId, ehrId);
        exchange.getMessage().setHeader(CompositionConstants.EHR_ID, ehrId);
    }

    public static String extractPatientId(String patientIdStr) {
        String regex = "(?:^|/)fhir/([^/]+/[^/]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(patientIdStr);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return patientIdStr;
    }
    
   /**
     * Creates an EHR for the given patient ID.
     *
     * @param patientId the given patient ID
     * @return the EHR ID
     */
    private UUID createOrGetPatientEhr(Patient patient, String patientId, String serverPatientId,String systemId) { //SPAGHETTI
        UUID ehrId;
        Identifier pseudonym = PatientUtils.getPseudonym(patient);
        Query<Record1<UUID>> query = Query.buildNativeQuery(
                "SELECT e/ehr_id/value from EHR e WHERE e/ehr_status/subject/external_ref/id/value = '" + pseudonym.getValue() + "'", UUID.class);
        List<Record1<UUID>> result = openEhrClient.aqlEndpoint().execute(query);
        if (result.isEmpty()) {
            LOG.debug("PatientId not found in EHR server: {} Creating EHRId", patientId);
            ehrId = (UUID) createEhr(pseudonym.getValue());
            savePatientEhr(patientId, serverPatientId, systemId, ehrId);
        
        } else if (result.size() > 1) {
            throw new ConversionException("Conflict: several EHR ids have the same patient id connected (subject.external_ref.id.value). Please check your EHR ids");
        } else {
            ehrId = result.get(0).value1();
            
            //TODO: Check the patientid-ehrid mapping is correct in the db
            // check if ehrid mapped to serverPatientId in db. Else throw error ??
  
            PatientEhr patientEhr = patientEhrRepository.findByInternalPatientIdAndEhrId(serverPatientId, ehrId);
            if (patientEhr == null) {
                // Create and save new mapping if none exists
                // A new patient could be created if
                // the input Subject reference Identifier was not found in Fhir server
                //TODO: Add identifier as input patientId?
                savePatientEhr(patientId, serverPatientId, systemId, ehrId);
            }
  
        }
        return ehrId;
    }


    private Object createEhr(String pseudonym) {
        PartySelf subject = new PartySelf(new PartyRef(new HierObjectId(pseudonym), "fhir-bridge", "PERSON"));
        EhrStatus ehrStatus = new EhrStatus(ARCHETYPE_NODE_ID, new DvText("EHR Status"), subject, true, true, null);
        return openEhrClient.ehrEndpoint().createEhr(ehrStatus);
    }

    private void savePatientEhr(String patientId, String serverPatientId,String systemId, UUID ehrId) {
        PatientEhr patientEhr = new PatientEhr(patientId, serverPatientId, systemId, ehrId);
        LocalDateTime dateTime = LocalDateTime.now();
        patientEhr.setCreatedDateTime(dateTime);
        patientEhr.setUpdatedDateTime(dateTime);    
        patientEhrRepository.save(patientEhr);
        LOG.debug("Saved PatientEhr: patientId={}, serverPatientId={}, ehrId={}", 
            patientEhr.getInputPatientId(), 
            patientEhr.getInternalPatientId(), 
            patientEhr.getEhrId());

    }
}
