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

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.component.ehr.composition.CompositionConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.apache.camel.Exchange;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record1;
import org.ehrbase.fhirbridge.fhir.support.PatientUtils;
import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.repository.PatientEhrRepository;
import org.ehrbase.fhirbridge.exception.ConversionException;
import org.ehrbase.fhirbridge.fhir.support.FhirUtils;
import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClient;
// import org.ehrbase.openehr.sdk.generator.commons.aql.query.Query;
// import org.ehrbase.openehr.sdk.generator.commons.aql.record.Record1;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
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
        Patient resource = (Patient) exchange.getIn().getHeader(CamelConstants.SERVER_PATIENT_RESOURCE);
        // String patientId = getPatientId(exchange);

        String patientIdStr = (String) exchange.getIn().getHeader(CamelConstants.SERVER_PATIENT_ID);
        String patientId = extractPatientId(patientIdStr);
        LOG.info("findById(patientId): " + patientId);
        UUID ehrId = patientEhrRepository.findById(patientId)
                .map(PatientEhr::getEhrId)
                .orElseGet(() -> createOrGetPatientEhr(resource, patientId));

        exchange.getMessage().setHeader(CompositionConstants.EHR_ID, ehrId);
    }

    public static String extractPatientId(String patientIdStr) {
        String regex = "/([^/]+/\\d+)/";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(patientIdStr);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return patientIdStr;
    }
    
    /**
     * Gets the current patient ID.
     *
     * @param exchange the current exchange
     * @return the patient ID
     */
    private String getPatientId(Exchange exchange) {
        String resourceStr = (String) exchange.getIn().getHeader(CamelConstants.INPUT_RESOURCE);
        String resourceType = FhirUtils.getResourceType(resourceStr);
        if (resourceType == ResourceType.Patient.name()) {
            Patient patientResource = (Patient) exchange.getIn().getHeader(CamelConstants.SERVER_PATIENT_RESOURCE);
            String patientId = patientResource.getId();
            exchange.setProperty("FHIRPatientId", patientId);
            return patientId;
        } else {
            return exchange.getIn().getHeader(CamelConstants.PATIENT_ID, String.class);
            // return exchange.getIn().getHeader(CamelConstants.PATIENT_ID, IIdType.class);
        }
    }

    /**
     * Creates an EHR for the given patient ID.
     *
     * @param patientId the given patient ID
     * @return the EHR ID
     */
    private UUID createOrGetPatientEhr(Patient patient, String patientId) { //SPAGHETTI
        UUID ehrId;
        Identifier pseudonym = PatientUtils.getPseudonym(patient);
        Query<Record1<UUID>> query = Query.buildNativeQuery(
                "SELECT e/ehr_id/value from EHR e WHERE e/ehr_status/subject/external_ref/id/value = '" + pseudonym.getValue() + "'", UUID.class);
        List<Record1<UUID>> result = openEhrClient.aqlEndpoint().execute(query);
        if (result.size() == 0) {
            LOG.debug("PatientId not found in EHR server" + patientId + "Creating EHRId");
            ehrId = (UUID) createEhr(pseudonym.getValue());
            PatientEhr patientEhr = new PatientEhr(patientId, ehrId);
            patientEhrRepository.save(patientEhr);
            LOG.debug("Created PatientEhr: patientId={}, ehrId={}", patientEhr.getPatientId(), patientEhr.getEhrId());
        } else {
            ehrId = getAlreadyExistingEHR(result);
            LOG.debug("PatientId found in EHR server" + patientId + " EHRId: " + ehrId);
        }
        return ehrId;
    }


    private UUID getAlreadyExistingEHR(List<Record1<UUID>> result) {
        if (result.size() > 1) {
            throw new ConversionException("Conflict: several EHR ids have the same patient id connected (subject.external_ref.id.value). Please check your EHR ids");
        }
        //Medblocks: Check the patientid-ehrid mapping is correct in the db
        return result.get(0).value1();
    }

    private Object createEhr(String pseudonym) {
        PartySelf subject = new PartySelf(new PartyRef(new HierObjectId(pseudonym), "fhir-bridge", "PERSON"));
        EhrStatus ehrStatus = new EhrStatus(ARCHETYPE_NODE_ID, new DvText("EHR Status"), subject, true, true, null);
        return openEhrClient.ehrEndpoint().createEhr(ehrStatus);
    }

}
