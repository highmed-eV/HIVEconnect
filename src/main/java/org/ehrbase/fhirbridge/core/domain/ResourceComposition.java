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

package org.ehrbase.fhirbridge.core.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "FB_RESOURCE_COMPOSITION", uniqueConstraints = @UniqueConstraint(columnNames = {"INPUT_RESOURCE_ID", "COMPOSITION_ID"}))
@EntityListeners(AuditingEntityListener.class)
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

    @Column(name = "COMPOSITION_ID", nullable = false)
    private String compositionId;

    @Column(name = "SYSTEM_ID")
    private String systemId;

    @Column(name = "CREATED_DATE_TIME", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreatedDate
    private ZonedDateTime createdDateTime;

    @Column(name = "UPDATED_DATE_TIME", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @LastModifiedDate
    private ZonedDateTime updatedDateTime;

    public ResourceComposition() {
    }

    public ResourceComposition(String inputResourceId) {
        this.inputResourceId = inputResourceId;
    }

    public ResourceComposition(String inputResourceId, String internalResourceId, String compositionId, String systemId) {
        this.inputResourceId = inputResourceId;
        this.internalResourceId = internalResourceId;
        this.compositionId = compositionId;
        this.systemId = systemId;
    }

    public String getInputResourceId() {
        return inputResourceId;
    }

    public void setInputResourceId(String id) {
        this.inputResourceId = id;
    }

    public String getInternalResourceId() {
        return internalResourceId;
    }

    public void setInternalResourceId(String internalResourceId) {
        this.internalResourceId = internalResourceId;
    }

    public String getCompositionId() {
        return compositionId;
    }

    public void setCompositionId(String versionUid) {
        this.compositionId = versionUid;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public ZonedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public ZonedDateTime getUpdatedDateTime() {
        return updatedDateTime;
    }

    @Override
    public String toString() {
        return "ResourceComposition{" +
                "id=" + id + '\'' +
                ", inputResourceId='" + inputResourceId + '\'' +
                ", internalResourceId='" + internalResourceId + '\'' +
                ", compositionId='" + compositionId + '\'' +
                ", systemId='" + systemId + '\'' +
                ", createdDateTime='" + createdDateTime + '\'' +
                ", updatedDateTime='" + updatedDateTime + '\'' +
                '}';
    }
}
