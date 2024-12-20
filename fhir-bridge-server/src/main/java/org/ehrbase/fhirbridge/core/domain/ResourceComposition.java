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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "FB_RESOURCE_COMPOSITION")
public class ResourceComposition {

    @Id
    @Column(name = "INPUT_RESOURCE_ID")
    private String inputResourceId;

    @Column(name = "INTERNAL_RESOURCE_ID")
    private String internalResourceId;

    @Column(name = "COMPOSITION_ID")
    private String compositionId;

    @Column(name = "SYSTEM_ID")
    private String systemId;

    public ResourceComposition() {
    }

    public ResourceComposition(String inputResourceId) {
        this.inputResourceId = inputResourceId;
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

    @Override
    public String toString() {
        return "ResourceComposition{" +
                "inputResourceId='" + inputResourceId + '\'' +
                ", internalResourceId='" + internalResourceId + '\'' +
                ", compositionId='" + compositionId + '\'' +
                ", systemId='" + systemId + '\'' +
                '}';
    }
}
