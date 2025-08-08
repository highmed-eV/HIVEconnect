package org.highmed.hiveconnect.core.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BootstrapEntityTest {

    @Test
    void testDefaultConstructor() {
        // Act
        BootstrapEntity bootstrapEntity = new BootstrapEntity();

        // Assert
        assertThat(bootstrapEntity.getId()).isNull();
        assertThat(bootstrapEntity.getFile()).isNull();
        assertThat(bootstrapEntity.getCreatedDateTime()).isNull();
        assertThat(bootstrapEntity.getUpdatedDateTime()).isNull();
    }

    @Test
    void testParameterizedConstructor() {
        // Arrange
        String file = "test.opt";

        // Act
        BootstrapEntity bootstrapEntity = new BootstrapEntity(file);

        // Assert
        assertThat(bootstrapEntity.getId()).isNull();
        assertThat(bootstrapEntity.getFile()).isEqualTo(file);
        assertThat(bootstrapEntity.getCreatedDateTime()).isNull();
        assertThat(bootstrapEntity.getUpdatedDateTime()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        BootstrapEntity bootstrapEntity = new BootstrapEntity();
        String id = "test-id";
        String file = "test.opt";
        LocalDateTime now = LocalDateTime.now();

        // Act
        bootstrapEntity.setId(id);
        bootstrapEntity.setFile(file);
        bootstrapEntity.setCreatedDateTime(now);
        bootstrapEntity.setUpdatedDateTime(now);

        // Assert
        assertThat(bootstrapEntity.getId()).isEqualTo(id);
        assertThat(bootstrapEntity.getFile()).isEqualTo(file);
        assertThat(bootstrapEntity.getCreatedDateTime()).isEqualTo(now);
        assertThat(bootstrapEntity.getUpdatedDateTime()).isEqualTo(now);
    }

    @Test
    void testSameObjectReference() {
        // Arrange
        BootstrapEntity bootstrapEntity = new BootstrapEntity("test.opt");
        BootstrapEntity sameReference = bootstrapEntity;

        // Assert
        assertThat(bootstrapEntity).isSameAs(sameReference);
    }

    @Test
    void testSameValues() {
        // Arrange
        String file = "test.opt";
        BootstrapEntity bootstrapEntity1 = new BootstrapEntity(file);
        BootstrapEntity bootstrapEntity2 = new BootstrapEntity(file);

        // Assert
        assertThat(bootstrapEntity1.getFile()).isEqualTo(bootstrapEntity2.getFile());
    }

    @Test
    void testDifferentValues() {
        // Arrange
        BootstrapEntity bootstrapEntity1 = new BootstrapEntity("test1.opt");
        BootstrapEntity bootstrapEntity2 = new BootstrapEntity("test2.opt");

        // Assert
        assertThat(bootstrapEntity1.getFile()).isNotEqualTo(bootstrapEntity2.getFile());
    }

} 