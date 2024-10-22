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

package org.ehrbase.fhirbridge.fhir.researchsubject;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.openehealth.ipf.commons.ihe.fhir.FhirTransactionConfiguration;
import org.openehealth.ipf.commons.ihe.fhir.FhirTransactionValidator;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirQueryAuditDataset;

/**
 * Configuration for 'Find Research Subject' transaction.
 *
 * @since 1.0.0
 */
public class FindResearchSubjectTransaction extends FhirTransactionConfiguration<FhirQueryAuditDataset> {

    public FindResearchSubjectTransaction() {
        super("research-subject-find",
                "Find Research Subject",
                true,
                null,
                new FindResearchSubjectAuditStrategy(),
                FhirVersionEnum.R4,
                new FindResearchSubjectProvider(),
                null,
                FhirTransactionValidator.NO_VALIDATION);
    }
}
