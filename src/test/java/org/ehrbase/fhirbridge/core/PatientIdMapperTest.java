package org.ehrbase.fhirbridge.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PatientIdMapperTest {

    private final PatientIdMapper patientIdMapper = new PatientIdMapper();

    @Test
    void testMapFhirPatientIdToEhrId() {
        // Arrange
        String fhirPatientId = "test-patient-123";

        // Act
        String ehrId = patientIdMapper.mapFhirPatientIdToEhrId(fhirPatientId);

        // Assert
        assertThat(ehrId).isEqualTo("9dd5ea8b-d9ed-4449-951d-6db59ca23fba");
    }

    @Test
    void testMapFhirPatientIdToEhrIdWithNullInput() {
        // Act
        String ehrId = patientIdMapper.mapFhirPatientIdToEhrId(null);

        // Assert
        assertThat(ehrId).isEqualTo("9dd5ea8b-d9ed-4449-951d-6db59ca23fba");
    }

    @Test
    void testMapFhirPatientIdToEhrIdWithEmptyInput() {
        // Act
        String ehrId = patientIdMapper.mapFhirPatientIdToEhrId("");

        // Assert
        assertThat(ehrId).isEqualTo("9dd5ea8b-d9ed-4449-951d-6db59ca23fba");
    }
} 