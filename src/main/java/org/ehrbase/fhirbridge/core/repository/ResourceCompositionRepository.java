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

import org.ehrbase.fhirbridge.core.domain.ResourceComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceCompositionRepository extends JpaRepository<ResourceComposition, String> {

    @Query("SELECT rc.inputResourceId FROM ResourceComposition rc WHERE rc.internalResourceId = :internalResourceId AND rc.systemId = :systemId")
    String findInternalResourceIdByInputResourceIdAndSystemId(@Param("internalResourceId") String internalResourceId, @Param("systemId") String systemId);

    @Query("SELECT rc.internalResourceId FROM ResourceComposition rc WHERE rc.inputResourceId IN :inputResourceIds AND rc.systemId = :systemId")
    List<String> findInternalResourceIdsByInputResourceIdsAndSystemId(@Param("inputResourceIds") List<String> inputResourceIds, @Param("systemId") String systemId);

    @Query("SELECT rc FROM ResourceComposition rc WHERE rc.inputResourceId = :inputResourceId AND rc.systemId = :systemId ORDER BY rc.id DESC LIMIT 1")
    Optional<ResourceComposition> findByInputResourceIdAndSystemId(@Param("inputResourceId") String inputResourceId, @Param("systemId") String systemId);

    Optional<ResourceComposition> findByInternalResourceIdAndCompositionIdAndEhrId(String internalResourceId, String compositionId, UUID ehrId);

    @Query("SELECT rc.compositionId FROM ResourceComposition rc WHERE rc.inputResourceId = :inputResourceId AND rc.ehrId = :ehrId")
    List<String> findCompositionIdsByInputResourceIdAndEhrId(@Param("inputResourceId") String inputResourceId, @Param("ehrId") UUID ehrId);

    @Query("SELECT rc.compositionId FROM ResourceComposition rc WHERE rc.internalResourceId = :internalResourceId AND rc.ehrId = :ehrId")
    List<String> findCompositionIdsByInternalResourceIdAndEhrId(@Param("internalResourceId") String internalResourceId, @Param("ehrId") UUID ehrId);

    @Query("SELECT rc.inputResourceId FROM ResourceComposition rc WHERE rc.compositionId = :compositionId AND rc.ehrId = :ehrId")
    List<String> findInputResourcesByCompositionIdAndEhrId(@Param("compositionId") String compositionId, @Param("ehrId") UUID ehrId);

    @Query("SELECT rc.internalResourceId FROM ResourceComposition rc WHERE rc.compositionId = :compositionId AND rc.ehrId = :ehrId")
    List<String> findInternalResourcesByCompositionIdAndEhrId(@Param("compositionId") String compositionId, @Param("ehrId") UUID ehrId);
}
