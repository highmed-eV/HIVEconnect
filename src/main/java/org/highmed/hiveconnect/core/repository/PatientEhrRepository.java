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

package org.highmed.hiveconnect.core.repository;

import java.util.UUID;

import org.highmed.hiveconnect.core.domain.PatientEhr;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientEhrRepository extends JpaRepository<PatientEhr, String> {
    PatientEhr findByInputPatientIdAndSystemId(String inputPatientId, String systemId);
    
    PatientEhr findByInternalPatientIdAndSystemId(String internalPatientId, String systemId);

    PatientEhr findByInternalPatientIdAndEhrId(String serverPatientId, UUID ehrId);
}
