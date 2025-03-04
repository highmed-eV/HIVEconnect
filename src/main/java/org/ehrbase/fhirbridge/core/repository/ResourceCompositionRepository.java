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

package org.ehrbase.fhirbridge.core.repository;

import java.util.Optional;

import org.ehrbase.fhirbridge.core.domain.PatientEhr;
import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceCompositionRepository extends JpaRepository<ResourceComposition, String> {

    @Query("SELECT rc.inputResourceId FROM ResourceComposition rc WHERE rc.internalResourceId = :internalResourceId")
    String findInternalResourceIdByInputResourceId(@Param("internalResourceId") String internalResourceId);

    @Query("SELECT rc.internalResourceId FROM ResourceComposition rc WHERE rc.inputResourceId IN :inputResourceIds")
    List<String> findInternalResourceIdsByInputResourceIds(@Param("inputResourceIds") List<String> inputResourceIds);

    @Query("SELECT rc FROM ResourceComposition rc WHERE rc.inputResourceId = :inputResourceId ORDER BY rc.id DESC LIMIT 1")
    Optional<ResourceComposition> findByInputResourceId(@Param("inputResourceId") String inputResourceId);

    Optional<ResourceComposition> findByInputResourceIdAndCompositionId(String inputResourceId, String compositionId);

    Optional<ResourceComposition> findByInternalResourceIdAndCompositionId(String internalResourceId, String compositionId);

    @Query("SELECT rc.compositionId FROM ResourceComposition rc WHERE rc.inputResourceId = :inputResourceId")
    List<String> findCompositionIdsByInputResourceId(@Param("inputResourceId") String inputResourceId);

    @Query("SELECT rc.compositionId FROM ResourceComposition rc WHERE rc.internalResourceId = :internalResourceId")
    List<String> findCompositionIdsByInternalResourceId(@Param("internalResourceId") String internalResourceId);

    @Query("SELECT rc.inputResourceId FROM ResourceComposition rc WHERE rc.compositionId = :compositionId")
    List<String> findInputResourcesByCompositionId(@Param("compositionId") String compositionId);

    @Query("SELECT rc.internalResourceId FROM ResourceComposition rc WHERE rc.compositionId = :compositionId")
    List<String> findInternalResourcesByCompositionId(@Param("compositionId") String compositionId);

    Optional<ResourceComposition> findByInternalResourceId(String internalResourceId);

}
