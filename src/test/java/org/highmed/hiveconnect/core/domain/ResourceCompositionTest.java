package org.highmed.hiveconnect.core.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ResourceCompositionTest {

    @Test
    void testDefaultConstructor() {
        // Act
        ResourceComposition resourceComposition = new ResourceComposition();

        // Assert
        assertThat(resourceComposition.getInputResourceId()).isNull();
        assertThat(resourceComposition.getInternalResourceId()).isNull();
        assertThat(resourceComposition.getEhrId()).isNull();
        assertThat(resourceComposition.getCompositionId()).isNull();
        assertThat(resourceComposition.getTemplateId()).isNull();
        assertThat(resourceComposition.getSystemId()).isNull();
        assertThat(resourceComposition.getCreatedDateTime()).isNull();
        assertThat(resourceComposition.getUpdatedDateTime()).isNull();
    }

    @Test
    void testSingleParameterConstructor() {
        // Arrange
        String inputResourceId = "input-123";

        // Act
        ResourceComposition resourceComposition = new ResourceComposition(inputResourceId);

        // Assert
        assertThat(resourceComposition.getInputResourceId()).isEqualTo(inputResourceId);
        assertThat(resourceComposition.getInternalResourceId()).isNull();
        assertThat(resourceComposition.getEhrId()).isNull();
        assertThat(resourceComposition.getCompositionId()).isNull();
        assertThat(resourceComposition.getTemplateId()).isNull();
        assertThat(resourceComposition.getSystemId()).isNull();
        assertThat(resourceComposition.getCreatedDateTime()).isNull();
        assertThat(resourceComposition.getUpdatedDateTime()).isNull();
    }

    @Test
    void testFullParameterConstructor() {
        // Arrange
        String inputResourceId = "input-123";
        String internalResourceId = "internal-123";
        String compositionId = "composition-123";
        String systemId = "system-123";
        String templateId = "template-123";

        // Act
        ResourceComposition resourceComposition = new ResourceComposition(inputResourceId, internalResourceId, compositionId, systemId, templateId);

        // Assert
        assertThat(resourceComposition.getInputResourceId()).isEqualTo(inputResourceId);
        assertThat(resourceComposition.getInternalResourceId()).isEqualTo(internalResourceId);
        assertThat(resourceComposition.getCompositionId()).isEqualTo(compositionId);
        assertThat(resourceComposition.getTemplateId()).isEqualTo(templateId);
        assertThat(resourceComposition.getSystemId()).isEqualTo(systemId);
        assertThat(resourceComposition.getEhrId()).isNull();
        assertThat(resourceComposition.getCreatedDateTime()).isNull();
        assertThat(resourceComposition.getUpdatedDateTime()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        ResourceComposition resourceComposition = new ResourceComposition();
        String inputResourceId = "input-123";
        String internalResourceId = "internal-123";
        String compositionId = "composition-123";
        String systemId = "system-123";
        String templateId = "template-123";
        UUID ehrId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // Act
        resourceComposition.setInputResourceId(inputResourceId);
        resourceComposition.setInternalResourceId(internalResourceId);
        resourceComposition.setCompositionId(compositionId);
        resourceComposition.setTemplateId(templateId);
        resourceComposition.setSystemId(systemId);
        resourceComposition.setEhrId(ehrId);
        resourceComposition.setCreatedDateTime(now);
        resourceComposition.setUpdatedDateTime(now);

        // Assert
        assertThat(resourceComposition.getInputResourceId()).isEqualTo(inputResourceId);
        assertThat(resourceComposition.getInternalResourceId()).isEqualTo(internalResourceId);
        assertThat(resourceComposition.getCompositionId()).isEqualTo(compositionId);
        assertThat(resourceComposition.getTemplateId()).isEqualTo(templateId);
        assertThat(resourceComposition.getSystemId()).isEqualTo(systemId);
        assertThat(resourceComposition.getEhrId()).isEqualTo(ehrId);
        assertThat(resourceComposition.getCreatedDateTime()).isEqualTo(now);
        assertThat(resourceComposition.getUpdatedDateTime()).isEqualTo(now);
    }

    @Test
    void testSameObjectReference() {
        // Arrange
        ResourceComposition resourceComposition = new ResourceComposition("input-123", "internal-123", "composition-123", "system-123", "template-123");
        ResourceComposition sameReference = resourceComposition;

        // Assert
        assertThat(resourceComposition).isSameAs(sameReference);
    }

    @Test
    void testSameValues() {
        // Arrange
        String inputResourceId = "input-123";
        String internalResourceId = "internal-123";
        String compositionId = "composition-123";
        String systemId = "system-123";
        String templateId = "template-123";
        ResourceComposition resourceComposition1 = new ResourceComposition(inputResourceId, internalResourceId, compositionId, systemId, templateId);
        ResourceComposition resourceComposition2 = new ResourceComposition(inputResourceId, internalResourceId, compositionId, systemId, templateId);

        // Assert
        assertThat(resourceComposition1.getInputResourceId()).isEqualTo(resourceComposition2.getInputResourceId());
        assertThat(resourceComposition1.getInternalResourceId()).isEqualTo(resourceComposition2.getInternalResourceId());
        assertThat(resourceComposition1.getCompositionId()).isEqualTo(resourceComposition2.getCompositionId());
        assertThat(resourceComposition1.getTemplateId()).isEqualTo(resourceComposition2.getTemplateId());
        assertThat(resourceComposition1.getSystemId()).isEqualTo(resourceComposition2.getSystemId());
        assertThat(resourceComposition1.getEhrId()).isEqualTo(resourceComposition2.getEhrId());
        assertThat(resourceComposition1.getCreatedDateTime()).isEqualTo(resourceComposition2.getCreatedDateTime());
        assertThat(resourceComposition1.getUpdatedDateTime()).isEqualTo(resourceComposition2.getUpdatedDateTime());
    }

    @Test
    void testDifferentValues() {
        // Arrange
        ResourceComposition resourceComposition1 = new ResourceComposition("input-123", "internal-123", "composition-123", "system-123", "template-123", UUID.randomUUID());
        ResourceComposition resourceComposition2 = new ResourceComposition("input-456", "internal-456", "composition-456", "system-456", "template-456", UUID.randomUUID());

        // Assert
        assertThat(resourceComposition1.getInputResourceId()).isNotEqualTo(resourceComposition2.getInputResourceId());
        assertThat(resourceComposition1.getInternalResourceId()).isNotEqualTo(resourceComposition2.getInternalResourceId());
        assertThat(resourceComposition1.getCompositionId()).isNotEqualTo(resourceComposition2.getCompositionId());
        assertThat(resourceComposition1.getTemplateId()).isNotEqualTo(resourceComposition2.getTemplateId());
        assertThat(resourceComposition1.getSystemId()).isNotEqualTo(resourceComposition2.getSystemId());
        assertThat(resourceComposition1.getEhrId()).isNotEqualTo(resourceComposition2.getEhrId());
    }

    @Test
    void testToString() {
        // Arrange
        ResourceComposition resourceComposition = new ResourceComposition("input-123", "internal-123", "composition-123", "system-123", "template-123");

        // Act
        String result = resourceComposition.toString();

        // Assert
        assertThat(result).contains("input-123");
        assertThat(result).contains("internal-123");
        assertThat(result).contains("composition-123");
        assertThat(result).contains("system-123");
        assertThat(result).contains("template-123");
    }
} 