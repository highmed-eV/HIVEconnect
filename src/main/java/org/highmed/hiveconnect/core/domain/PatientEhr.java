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

package org.highmed.hiveconnect.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "FB_PATIENT_EHR")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PatientEhr {

    @Id
    @Column(name = "INPUT_PATIENT_ID")
    private String inputPatientId;

    @Column(name = "INTERNAL_PATIENT_ID")
    private String internalPatientId;

    @Column(name = "SYSTEM_ID")
    private String systemId;

    @NotNull
    @Column(name = "EHR_ID")
    private UUID ehrId;

    @Column(name = "CREATED_DATE_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdDateTime;

    @Column(name = "UPDATED_DATE_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedDateTime;

    public PatientEhr() {
    }

    public PatientEhr(String inputPatientId, 
                    String internalPatientId,
                    String systemId,
                    UUID ehrId) {
        this.inputPatientId = inputPatientId;
        this.internalPatientId = internalPatientId;
        this.systemId = systemId;
        this.ehrId = ehrId;
    }

    @Override
    public String toString() {
        return "PatientEhr{" +
                "inputPatientId='" + inputPatientId + '\'' +
                "internalPatientId='" + internalPatientId + '\'' +
                "systemId='" + systemId + '\'' +
                ", ehrId=" + ehrId +
                ", createdDateTime=" + createdDateTime +
                ", updatedDateTime=" + updatedDateTime +
                '}';
    }
}
