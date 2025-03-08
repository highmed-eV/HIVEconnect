package org.ehrbase.fhirbridge.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Entity
@Table(name = "bootstrap")
@EntityListeners(AuditingEntityListener.class)
public class BootstrapEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id")
    private String id;

    @Column(name = "file", nullable = false)
    private String file;

    @CreatedDate
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private ZonedDateTime createdDateTime;

    @LastModifiedDate
    @Column(name = "updated_date_time")
    private ZonedDateTime updatedDateTime;

    public BootstrapEntity() {
    }

    public BootstrapEntity(String file) {
        this.file = file;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public ZonedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public ZonedDateTime getUpdatedDateTime() {
        return updatedDateTime;
    }
} 