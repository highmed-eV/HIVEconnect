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

import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.archetyped.FeederAuditDetails;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rm.support.identification.TerminologyId;

import org.apache.camel.Exchange;
import org.ehrbase.fhirbridge.camel.CamelConstants;
import org.ehrbase.fhirbridge.camel.processor.FhirRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.apache.camel.Processor Processor} that retrieves the EHR ID of the patient involved in the current
 * exchange. If the patient does not have an EHR, a new one is created and the EHR ID is stored in the database.
 *
 * @since 1.2.0
 */
@Component(CompositionContextProcessor.BEAN_ID)
@SuppressWarnings("java:S6212")
public class CompositionContextProcessor implements FhirRequestProcessor {

    public static final String BEAN_ID = "compositionContextProcessor";

    public static final String DEFAULT_SYSTEM_ID = "FHIR-Bridge";

    private static final Logger LOG = LoggerFactory.getLogger(CompositionContextProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Composition composition = (Composition) exchange.getIn().getBody();
        String source = (String) exchange.getMessage().getHeader(CamelConstants.FHIR_INPUT_SOURCE);
        String resourceId = (String) exchange.getMessage().getHeader(CamelConstants.FHIR_SERVER_RESOURCE_ID);

        TerminologyId terminologyId = new TerminologyId("openehr");
        CodePhrase definingCode = new CodePhrase(terminologyId, "433");
        DvCodedText category = new DvCodedText("event", definingCode);
        composition.setCategory(category);
        composition.setFeederAudit(buildFeederAudit(source, resourceId));
        
        TerminologyId langTerminologyId = new TerminologyId("ISO_639-1");
        CodePhrase langDefiningCode = new CodePhrase(langTerminologyId, "de");
        composition.setLanguage(langDefiningCode);
        
        TerminologyId territoryTerminologyId = new TerminologyId("ISO_3166-1");
        CodePhrase territoryDefiningCode = new CodePhrase(territoryTerminologyId, "DE");
        composition.setTerritory(territoryDefiningCode);
        
        if (composition.getComposer() == null) {
            PartyProxy composer = new PartySelf();
            PartyRef compoPartyRef = new PartyRef ();
            compoPartyRef.setNamespace("openFHIR");
            composer.setExternalRef(compoPartyRef);
            composition.setComposer(composer);
        }
        // convertHealthCareFacility(resource).ifPresent(composition::setHealthCareFacility);

        exchange.getIn().setBody(composition);
    }

    protected FeederAudit buildFeederAudit(String source, String resourceId) {
        FeederAudit result = new FeederAudit();
        String systemId = source != null ? source : DEFAULT_SYSTEM_ID;
        result.setOriginatingSystemAudit(new FeederAuditDetails(systemId, null, null, null, null, null, null));

        List<DvIdentifier> identifiers = new ArrayList<>();

        if (resourceId != null) {
            DvIdentifier identifier = new DvIdentifier();
            identifier.setId(resourceId);
            identifier.setType("fhir_logical_id");
            identifiers.add(identifier);
        }

        result.setOriginatingSystemItemIds(identifiers);
        return result;
    }

}
