package org.highmed.hiveconnect.core.repository;

import org.highmed.hiveconnect.core.domain.BootstrapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BootstrapRepository extends JpaRepository<BootstrapEntity, String> {
    Optional<BootstrapEntity> findByFile(String file);
} 