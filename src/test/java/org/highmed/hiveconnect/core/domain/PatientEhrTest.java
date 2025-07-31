package org.highmed.hiveconnect.core.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PatientEhrTest {

    @Test
    void testDefaultConstructor() {
        // Act
        PatientEhr patientEhr = new PatientEhr();

        // Assert
        assertThat(patientEhr.getInputPatientId()).isNull();
        assertThat(patientEhr.getInternalPatientId()).isNull();
        assertThat(patientEhr.getSystemId()).isNull();
        assertThat(patientEhr.getEhrId()).isNull();
        assertThat(patientEhr.getCreatedDateTime()).isNull();
        assertThat(patientEhr.getUpdatedDateTime()).isNull();
    }

    @Test
    void testParameterizedConstructor() {
        // Arrange
        String inputPatientId = "input-123";
        String internalPatientId = "internal-123";
        String systemId = "system-123";
        UUID ehrId = UUID.randomUUID();

        // Act
        PatientEhr patientEhr = new PatientEhr(inputPatientId, internalPatientId, systemId, ehrId);

        // Assert
        assertThat(patientEhr.getInputPatientId()).isEqualTo(inputPatientId);
        assertThat(patientEhr.getInternalPatientId()).isEqualTo(internalPatientId);
        assertThat(patientEhr.getSystemId()).isEqualTo(systemId);
        assertThat(patientEhr.getEhrId()).isEqualTo(ehrId);
        assertThat(patientEhr.getCreatedDateTime()).isNull();
        assertThat(patientEhr.getUpdatedDateTime()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        PatientEhr patientEhr = new PatientEhr();
        String inputPatientId = "input-123";
        String internalPatientId = "internal-123";
        String systemId = "system-123";
        UUID ehrId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // Act
        patientEhr.setInputPatientId(inputPatientId);
        patientEhr.setInternalPatientId(internalPatientId);
        patientEhr.setSystemId(systemId);
        patientEhr.setEhrId(ehrId);
        patientEhr.setCreatedDateTime(now);
        patientEhr.setUpdatedDateTime(now);

        // Assert
        assertThat(patientEhr.getInputPatientId()).isEqualTo(inputPatientId);
        assertThat(patientEhr.getInternalPatientId()).isEqualTo(internalPatientId);
        assertThat(patientEhr.getSystemId()).isEqualTo(systemId);
        assertThat(patientEhr.getEhrId()).isEqualTo(ehrId);
        assertThat(patientEhr.getCreatedDateTime()).isEqualTo(now);
        assertThat(patientEhr.getUpdatedDateTime()).isEqualTo(now);
    }

    @Test
    void testSameObjectReference() {
        // Arrange
        PatientEhr patientEhr = new PatientEhr("input-123", "internal-123", "system-123", UUID.randomUUID());
        PatientEhr sameReference = patientEhr;

        // Assert
        assertThat(patientEhr).isSameAs(sameReference);
    }

    @Test
    void testSameValues() {
        // Arrange
        String inputPatientId = "input-123";
        String internalPatientId = "internal-123";
        String systemId = "system-123";
        UUID ehrId = UUID.randomUUID();
        PatientEhr patientEhr1 = new PatientEhr(inputPatientId, internalPatientId, systemId, ehrId);
        PatientEhr patientEhr2 = new PatientEhr(inputPatientId, internalPatientId, systemId, ehrId);

        // Assert
        assertThat(patientEhr1.getInputPatientId()).isEqualTo(patientEhr2.getInputPatientId());
        assertThat(patientEhr1.getInternalPatientId()).isEqualTo(patientEhr2.getInternalPatientId());
        assertThat(patientEhr1.getSystemId()).isEqualTo(patientEhr2.getSystemId());
        assertThat(patientEhr1.getEhrId()).isEqualTo(patientEhr2.getEhrId());
        assertThat(patientEhr1.getCreatedDateTime()).isEqualTo(patientEhr2.getCreatedDateTime());
        assertThat(patientEhr1.getUpdatedDateTime()).isEqualTo(patientEhr2.getUpdatedDateTime());
    }

    @Test
    void testDifferentValues() {
        // Arrange
        PatientEhr patientEhr1 = new PatientEhr("input-123", "internal-123", "system-123", UUID.randomUUID());
        PatientEhr patientEhr2 = new PatientEhr("input-456", "internal-456", "system-456", UUID.randomUUID());

        // Assert
        assertThat(patientEhr1.getInputPatientId()).isNotEqualTo(patientEhr2.getInputPatientId());
        assertThat(patientEhr1.getInternalPatientId()).isNotEqualTo(patientEhr2.getInternalPatientId());
        assertThat(patientEhr1.getSystemId()).isNotEqualTo(patientEhr2.getSystemId());
        assertThat(patientEhr1.getEhrId()).isNotEqualTo(patientEhr2.getEhrId());
    }

    @Test
    void testToString() {
        // Arrange
        PatientEhr patientEhr = new PatientEhr("input-123", "internal-123", "system-123", UUID.randomUUID());

        // Act
        String result = patientEhr.toString();

        // Assert
        assertThat(result).contains("input-123");
        assertThat(result).contains("internal-123");
        assertThat(result).contains("system-123");
    }
} 