package org.ehrbase.fhirbridge.exception.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExceptionDtoTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        int id = 123;
        List<String> argumentsList = Arrays.asList("arg1", "arg2");

        // Act
        ExceptionDto dto = new ExceptionDto(id, argumentsList);

        // Assert
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getArgumentsList()).isEqualTo(argumentsList);
    }

    @Test
    void testWithEmptyArgumentsList() {
        // Arrange
        int id = 456;
        List<String> argumentsList = Collections.emptyList();

        // Act
        ExceptionDto dto = new ExceptionDto(id, argumentsList);

        // Assert
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getArgumentsList()).isEmpty();
    }

    @Test
    void testWithNullArgumentsList() {
        // Arrange
        int id = 789;
        List<String> argumentsList = null;

        // Act
        ExceptionDto dto = new ExceptionDto(id, argumentsList);

        // Assert
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getArgumentsList()).isNull();
    }

    
    @Test
    void testNotEqualsWithDifferentId() {
        // Arrange
        List<String> argumentsList = Arrays.asList("arg1", "arg2");
        ExceptionDto dto1 = new ExceptionDto(123, argumentsList);
        ExceptionDto dto2 = new ExceptionDto(456, argumentsList);

        // Assert
        assertThat(dto1).isNotEqualTo(dto2);
        assertThat(dto1.hashCode()).isNotEqualTo(dto2.hashCode());
    }

    @Test
    void testNotEqualsWithDifferentArguments() {
        // Arrange
        int id = 123;
        ExceptionDto dto1 = new ExceptionDto(id, Arrays.asList("arg1", "arg2"));
        ExceptionDto dto2 = new ExceptionDto(id, Arrays.asList("arg1", "arg3"));

        // Assert
        assertThat(dto1).isNotEqualTo(dto2);
        assertThat(dto1.hashCode()).isNotEqualTo(dto2.hashCode());
    }

} 