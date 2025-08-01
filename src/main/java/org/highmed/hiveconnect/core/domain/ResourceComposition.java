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

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "FB_RESOURCE_COMPOSITION", uniqueConstraints = @UniqueConstraint(columnNames = {"INPUT_RESOURCE_ID", "COMPOSITION_ID", "SYSTEM_ID", "EHR_ID"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ResourceComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_composition_id_generator")
    @SequenceGenerator(name = "resource_composition_id_generator", sequenceName = "resource_composition_id_seq", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "INPUT_RESOURCE_ID", nullable = false)
    private String inputResourceId;

    @Column(name = "INTERNAL_RESOURCE_ID")
    private String internalResourceId;

    @Column(name = "EHR_ID", nullable = false)
    private UUID ehrId;

    @Column(name = "COMPOSITION_ID", nullable = false)
    private String compositionId;

    @Column(name = "TEMPLATE_ID")
    private String templateId;

    @Column(name = "SYSTEM_ID", nullable = false)
    private String systemId;

    @Column(name = "CREATED_DATE_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdDateTime;
    
    @Column(name = "UPDATED_DATE_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedDateTime;

    public ResourceComposition() {
    }

    public ResourceComposition(String inputResourceId) {
        this.inputResourceId = inputResourceId;
    }

    public ResourceComposition(String inputResourceId, String internalResourceId, String compositionId, String systemId, String templateId) {
        this.inputResourceId = inputResourceId;
        this.internalResourceId = internalResourceId;
        this.compositionId = compositionId;
        this.systemId = systemId;
        this.templateId = templateId;
    }
    public ResourceComposition(String inputResourceId, String internalResourceId, String compositionId, String systemId, String templateId, UUID ehrId) {
        this.inputResourceId = inputResourceId;
        this.internalResourceId = internalResourceId;
        this.compositionId = compositionId;
        this.systemId = systemId;
        this.templateId = templateId;
        this.ehrId = ehrId;
    }

    @Override
    public String toString() {
        return "ResourceComposition{" +
                "id=" + id + '\'' +
                ", inputResourceId='" + inputResourceId + '\'' +
                ", internalResourceId='" + internalResourceId + '\'' +
                ", compositionId='" + compositionId + '\'' +
                ", templateId='" + templateId + '\'' +
                ", ehrId='" + ehrId + '\'' +
                ", systemId='" + systemId + '\'' +
                ", createdDateTime='" + createdDateTime + '\'' +
                ", updatedDateTime='" + updatedDateTime + '\'' +
                '}';
    }
}
