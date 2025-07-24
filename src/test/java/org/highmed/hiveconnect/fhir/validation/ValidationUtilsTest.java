package org.highmed.hiveconnect.fhir.validation;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ValidationUtilsTest {

    @Test
    void testAddInfo() {
        // Arrange
        OperationOutcome outcome = new OperationOutcome();
        String diagnostics = "Test info message";
        String location = "test.location";

        // Act
        ValidationUtils.addInfo(outcome, diagnostics, location);

        // Assert
        assertThat(outcome.getIssue()).hasSize(1);
        var issue = outcome.getIssueFirstRep();
        assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.WARNING);
        assertThat(issue.getDiagnostics()).isEqualTo(diagnostics);
        assertThat(issue.getLocation()).hasSize(1);
        assertThat(issue.getLocation().get(0).getValue()).isEqualTo(location);
    }

    @Test
    void testAddWarning() {
        // Arrange
        OperationOutcome outcome = new OperationOutcome();
        String diagnostics = "Test warning message";
        String location = "test.location";

        // Act
        ValidationUtils.addWarning(outcome, diagnostics, location);

        // Assert
        assertThat(outcome.getIssue()).hasSize(1);
        var issue = outcome.getIssueFirstRep();
        assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.WARNING);
        assertThat(issue.getDiagnostics()).isEqualTo(diagnostics);
        assertThat(issue.getLocation()).hasSize(1);
        assertThat(issue.getLocation().get(0).getValue()).isEqualTo(location);
    }

    @Test
    void testAddError() {
        // Arrange
        OperationOutcome outcome = new OperationOutcome();
        String diagnostics = "Test error message";
        String location = "test.location";

        // Act
        ValidationUtils.addError(outcome, diagnostics, location);

        // Assert
        assertThat(outcome.getIssue()).hasSize(1);
        var issue = outcome.getIssueFirstRep();
        assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.ERROR);
        assertThat(issue.getDiagnostics()).isEqualTo(diagnostics);
        assertThat(issue.getLocation()).hasSize(1);
        assertThat(issue.getLocation().get(0).getValue()).isEqualTo(location);
    }

    @Test
    void testAddIssue() {
        // Arrange
        OperationOutcome outcome = new OperationOutcome();
        String diagnostics = "Test issue message";
        String location = "test.location";

        // Act
        ValidationUtils.addIssue(outcome, IssueSeverity.WARNING, diagnostics, location);

        // Assert
        assertThat(outcome.getIssue()).hasSize(1);
        var issue = outcome.getIssueFirstRep();
        assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.WARNING);
        assertThat(issue.getDiagnostics()).isEqualTo(diagnostics);
        assertThat(issue.getLocation()).hasSize(1);
        assertThat(issue.getLocation().get(0).getValue()).isEqualTo(location);
    }

    @Test
    void testAddIssueWithNullLocation() {
        // Arrange
        OperationOutcome outcome = new OperationOutcome();
        String diagnostics = "Test issue message";

        // Act
        ValidationUtils.addIssue(outcome, IssueSeverity.WARNING, diagnostics, null);

        // Assert
        assertThat(outcome.getIssue()).hasSize(1);
        var issue = outcome.getIssueFirstRep();
        assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.WARNING);
        assertThat(issue.getDiagnostics()).isEqualTo(diagnostics);
        assertThat(issue.getLocation()).isEmpty();
    }

    @Test
    void testAddIssueWithNullOutcome() {
        // Act & Assert
        assertThatThrownBy(() -> ValidationUtils.addIssue(null, IssueSeverity.WARNING, "test", "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("OperationOutcome must not be null");
    }

    @Test
    void testAddIssueWithNullSeverity() {
        // Arrange
        OperationOutcome outcome = new OperationOutcome();

        // Act & Assert
        assertThatThrownBy(() -> ValidationUtils.addIssue(outcome, null, "test", "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("IssueSeverity must not be null");
    }

    @Test
    void testMultipleIssues() {
        // Arrange
        OperationOutcome outcome = new OperationOutcome();

        // Act
        ValidationUtils.addInfo(outcome, "Info message", "info.location");
        ValidationUtils.addWarning(outcome, "Warning message", "warning.location");
        ValidationUtils.addError(outcome, "Error message", "error.location");

        // Assert
        assertThat(outcome.getIssue()).hasSize(3);
        
        var infoIssue = outcome.getIssue().get(0);
        assertThat(infoIssue.getSeverity()).isEqualTo(IssueSeverity.WARNING);
        assertThat(infoIssue.getDiagnostics()).isEqualTo("Info message");
        assertThat(infoIssue.getLocation().get(0).getValue()).isEqualTo("info.location");

        var warningIssue = outcome.getIssue().get(1);
        assertThat(warningIssue.getSeverity()).isEqualTo(IssueSeverity.WARNING);
        assertThat(warningIssue.getDiagnostics()).isEqualTo("Warning message");
        assertThat(warningIssue.getLocation().get(0).getValue()).isEqualTo("warning.location");

        var errorIssue = outcome.getIssue().get(2);
        assertThat(errorIssue.getSeverity()).isEqualTo(IssueSeverity.ERROR);
        assertThat(errorIssue.getDiagnostics()).isEqualTo("Error message");
        assertThat(errorIssue.getLocation().get(0).getValue()).isEqualTo("error.location");
    }
} 