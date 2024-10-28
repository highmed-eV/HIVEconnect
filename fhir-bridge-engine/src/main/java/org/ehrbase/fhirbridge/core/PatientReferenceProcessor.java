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

 package org.ehrbase.fhirbridge.core;

// import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
// import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
// import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.adapter.FHIRAdapter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Specimen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link org.apache.camel.Processor Processor} that retrieves the patient associated to the submitted resource.
 * If no patient is found, a new one is created.
 *
 * @since 1.2.0
 */
@Component(PatientReferenceProcessor.BEAN_ID)
@SuppressWarnings("java:S6212")
public class PatientReferenceProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "patientReferenceProcessor";

    private static final Logger LOG = LoggerFactory.getLogger(PatientReferenceProcessor.class);

    private final FHIRAdapter fhirAdapter;

    public PatientReferenceProcessor(FHIRAdapter fhirAdapter) {
        this.fhirAdapter = fhirAdapter;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // RequestDetails requestDetails = getRequestDetails(exchange);
        // Resource resource = exchange.getIn().getBody(Resource.class);

        // if (!isPatient(resource)) {
        //     Reference subject = getSubject(resource)
        //             .orElseThrow(() -> new UnprocessableEntityException(requestDetails.getResourceName() + " should be linked to a subject/patient"));
        //     IIdType patientId;
        //     if (subject.hasReference()) {
        //         patientId = processReference(subject, requestDetails);
        //     } else if (hasIdentifier(subject)) {
        //         patientId = handleSubjectIdentifier(subject, requestDetails);
        //     } else {
        //         throw new UnprocessableEntityException("Subject identifier is required");
        //     }

        //     exchange.getIn().setHeader(CamelConstants.PATIENT_ID, patientId);
        // }
    }

    // public static boolean isPatient(Resource resource) {
    //     return resource != null && resource.getResourceType() == ResourceType.Patient;
    // }


    // private IIdType processReference(Reference subject, RequestDetails requestDetails) {
    //     if (subject.getResource() != null) {
    //         return handleSubjectReferenceInternal(subject, requestDetails);
    //     } else {
    //         return handleSubjectReferenceRemote(subject, requestDetails);
    //     }
    // }

    // private IIdType handleSubjectReferenceRemote(Reference subject, RequestDetails requestDetails) {
    //     IIdType patientId;
    //     //??
    //     Patient patientReference = fhirAdapter.read(subject.getReferenceElement());
    //     subject.setResource(patientReference);
    //     Identifier identifier = patientReference.getIdentifier().get(0); //TODO bad practice
    //     SearchParameterMap parameters = new SearchParameterMap();
    //     parameters.add(Patient.SP_IDENTIFIER, new TokenParam(identifier.getSystem(), identifier.getValue()));
    //     //??
    //     Set<ResourcePersistentId> ids = fhirAdapter.searchForIds(parameters, requestDetails);
    //     if (ids.size() == 1) {
    //         IBundleProvider bundleProvider = fhirAdapter.search(parameters, requestDetails);
    //         List<IBaseResource> result = bundleProvider.getResources(0, 1);
    //         Patient patient = (Patient) result.get(0);
    //         patientId = patient.getIdElement();
    //         LOG.debug("Resolved existing Patient: id={}", patientId);
    //     } else {
    //         throw new UnprocessableEntityException("More than one patient matching the given identifier system and value");
    //     }
    //     subject.setResource(patientReference);
    //     return patientId;
    // }


    // private IIdType handleSubjectReferenceInternal(Reference subject, RequestDetails requestDetails) {
    //     Patient patientReference = (Patient) subject.getResource();
    //     Identifier identifier =  patientReference.getIdentifier().get(0); //TODO bad practice
    //     SearchParameterMap parameters = new SearchParameterMap();
    //     parameters.add(Patient.SP_IDENTIFIER, new TokenParam(identifier.getSystem(), identifier.getValue()));
    //     Set<ResourcePersistentId> ids = fhirAdapter.searchForIds(parameters, requestDetails);
    //     IIdType patientId;
    //     if (ids.isEmpty()) {
    //         patientId = createPatient(identifier, requestDetails);
    //     } else if (ids.size() == 1) {
    //         IBundleProvider bundleProvider = fhirAdapter.search(parameters, requestDetails);
    //         List<IBaseResource> result = bundleProvider.getResources(0, 1);
    //         Patient patient = (Patient) result.get(0);
    //         patientId = patient.getIdElement();
    //         LOG.debug("Resolved existing Patient: id={}", patientId);
    //     } else {
    //         throw new UnprocessableEntityException("More than one patient matching the given identifier system and value");
    //     }
    //     subject.setReferenceElement(patientId);
    //     return patientId;


    // }

    // /**
    //  * Retrieves the patient ID corresponding the given subject. If there is no matching {@link Patient} exist,
    //  * a new one is created.
    //  *
    //  * @param subject        the current subject
    //  * @param requestDetails the current request details
    //  * @return the patient ID
    //  */
    // private IIdType handleSubjectIdentifier(Reference subject, RequestDetails requestDetails) {
    //     Identifier identifier = subject.getIdentifier();
    //     SearchParameterMap parameters = new SearchParameterMap();
    //     parameters.add(Patient.SP_IDENTIFIER, new TokenParam(identifier.getSystem(), identifier.getValue()));
    //     Set<ResourcePersistentId> ids = fhirAdapter.searchForIds(parameters, requestDetails);

    //     IIdType patientId;
    //     if (ids.isEmpty()) {
    //         patientId = createPatient(identifier, requestDetails);
    //     } else if (ids.size() == 1) {
    //         IBundleProvider bundleProvider = fhirAdapter.search(parameters, requestDetails);
    //         List<IBaseResource> result = bundleProvider.getResources(0, 1);
    //         Patient patient = (Patient) result.get(0);
    //         patientId = patient.getIdElement();
    //         LOG.debug("Resolved existing Patient: id={}", patientId);
    //     } else {
    //         throw new UnprocessableEntityException("More than one patient matching the given identifier system and value");
    //     }

    //     subject.setReferenceElement(patientId);

    //     return patientId;
    // }

    // /**
    //  * Creates a new dummy patient with the given identifier system and value.
    //  *
    //  * @param identifier     the identifier of the new patient.
    //  * @param requestDetails the current request details
    //  * @return the patient ID
    //  */
    // private IIdType createPatient(Identifier identifier, RequestDetails requestDetails) {
    //     IIdType id = fhirAdapter.create(new Patient().addIdentifier(identifier), requestDetails).getId();
    //     LOG.debug("Created Patient: id={}", id);
    //     return id;
    // }

    // private boolean hasIdentifier(Reference subject) {
    //     return subject.hasIdentifier() &&
    //             subject.getIdentifier().hasSystem() &&
    //             subject.getIdentifier().hasValue();
    // }

    // public static boolean isReferenceType(Reference reference, ResourceType resourceType) {
    //     if (reference == null || resourceType == null) {
    //         return false;
    //     }
    //     return reference.hasType() && reference.getType().equals(resourceType.name());
    // }

    // //replace with generic getSubject
    // public static Optional<Reference> getSubject(Resource resource) {
    //     switch (resource.getResourceType()) {
    //         case Condition:
    //             return getSubject((Condition) resource);
    //         case Consent:
    //             return getPatient((Consent) resource);
    //         case DiagnosticReport:
    //             return getSubject((DiagnosticReport) resource);
    //         case DocumentReference:
    //             return getSubject((DocumentReference) resource);
    //         case Encounter:
    //             return getSubject((Encounter) resource);
    //         case Immunization:
    //             return getPatient((Immunization) resource);
    //         case MedicationStatement:
    //             return getSubject((MedicationStatement) resource);
    //         case Observation:
    //             return getSubject((Observation) resource);
    //         case Procedure:
    //             return getSubject((Procedure) resource);
    //         case QuestionnaireResponse:
    //             return getSubject((QuestionnaireResponse) resource);
    //         case Specimen:
    //             return getSubject((Specimen) resource);
    //         case Composition:
    //             return getSubject((Composition) resource);
    //         default:
    //             throw new IllegalArgumentException("Unsupported resource type: " + resource.getResourceType());
    //     }
    // }
}
