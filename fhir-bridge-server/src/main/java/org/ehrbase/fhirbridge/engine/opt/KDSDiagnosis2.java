package org.ehrbase.fhirbridge.engine.opt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KDSDiagnosis2 {

    // Diagnose category
    private String categoryTerminology;
    private String categoryValue;
    private String categoryCode;

    // Diagnose context
    private String reportId;
    private String status;
    private String caseIdentifier;
    private UUID caseUid;
    private String problemDiagnosisQualifierTerminology;
    private String problemDiagnosisQualifierCode;
    private String problemDiagnosisQualifierValue;
    private UUID problemDiagnosisQualifierUid;
    private LocalDateTime startTime;
    private String settingCode;
    private String settingTerminology;
    private String settingValue;
    private LocalDateTime endTime;
    private String healthCareFacilityName;

    // Primary code
    private String primaryCodeIcd10GmCode;
    private String primaryCodeIcd10GmTerminology;
    private String primaryCodeIcd10GmValue;
    private String primaryCodeAlphaIdTerminology;
    private String primaryCodeAlphaIdValue;
    private String primaryCodeAlphaIdCode;
    private String primaryCodeSctValue;
    private String primaryCodeSctCode;
    private String primaryCodeSctTerminology;
    private String primaryCodeOrphanetValue;
    private String primaryCodeOrphanetTerminology;
    private String primaryCodeOrphanetCode;
    private String primaryCodeIcdO3Value;
    private String primaryCodeIcdO3Code;
    private String primaryCodeIcdO3Terminology;
    private String primaryCodeFreeTextDescription;
    private String primaryCodeBodySiteSnomedCtValue;
    private String primaryCodeBodySiteSnomedCtCode;
    private String primaryCodeBodySiteSnomedCtTerminology;
    private String primaryCodeBodySiteIcdO3Value;
    private String primaryCodeBodySiteIcdO3Code;
    private String primaryCodeBodySiteIcdO3Terminology;
    private String primaryCodeAnatomicalLocationBodySiteName;
    private String primaryCodeAnatomicalLocationLateralityCode;
    private String primaryCodeAnatomicalLocationLateralityTerminology;
    private String primaryCodeAnatomicalLocationLateralityValue;
    private UUID primaryCodeAnatomicalLocationUid;
    private LocalDateTime primaryCodeClinicallyRelevantPeriodTimeOfOccurrence;
    private LocalDateTime primaryCodeDeterminationDate;
    private String primaryCodeSeverityTerminology;
    private String primaryCodeSeverityCode;
    private String primaryCodeSeverityValue;
    private String primaryCodeLifePhaseBeginnTerminology;
    private String primaryCodeLifePhaseBeginnCode;
    private String primaryCodeLifePhaseBeginnValue;
    private String primaryCodeLifePhaseEndeCode;
    private String primaryCodeLifePhaseEndeValue;
    private UUID primaryCodeLifePhaseUid;
    private String primaryCodeMultipleCodingIdentifierCode;
    private String primaryCodeMultipleCodingIdentifierTerminology;
    private String primaryCodeMultipleCodingIdentifierValue;
    private UUID primaryCodeMultipleCodingIdentifierUid;
    private LocalDateTime primaryCodeClinicallyRelevantPeriodTimeOfRecovery;
    private String primaryCodeDiagnosticReliabilityCode;
    private String primaryCodeDiagnosticReliabilityTerminology;
    private String primaryCodeDiagnosticReliabilityValue;
    private String primaryCodeDiagnosticExplanation;
    private String primaryCodeClinicalStatusValue;
    private String primaryCodeClinicalStatusTerminology;
    private String primaryCodeClinicalStatusCode;
    private UUID primaryCodeClinicalStatusUid;
    private LocalDateTime primaryCodeLastDocumentationDate;
    private String primaryCodeLanguageCode;
    private String primaryCodeLanguageTerminology;
    private String primaryCodeEncodingTerminology;
    private String primaryCodeEncodingCode;

    // Secondary code
    private String secondaryCodeIcd10GmValue;
    private String secondaryCodeIcd10GmTerminology;
    private String secondaryCodeIcd10GmCode;
    private String secondaryCodeAlphaIdTerminology;
    private String secondaryCodeAlphaIdCode;
    private String secondaryCodeAlphaIdValue;
    private String secondaryCodeSctTerminology;
    private String secondaryCodeSctValue;
    private String secondaryCodeSctCode;
    private String secondaryCodeOrphanetTerminology;
    private String secondaryCodeOrphanetCode;
    private String secondaryCodeOrphanetValue;
    private String secondaryCodeIcdO3Terminology;
    private String secondaryCodeIcdO3Code;
    private String secondaryCodeIcdO3Value;
    private String secondaryCodeFreeTextDescription;
    private String secondaryCodeBodySiteSnomedCtValue;
    private String secondaryCodeBodySiteSnomedCtTerminology;
    private String secondaryCodeBodySiteSnomedCtCode;
    private String secondaryCodeBodySiteIcdO3Terminology;
    private String secondaryCodeBodySiteIcdO3Code;
    private String secondaryCodeBodySiteIcdO3Value;
    private String secondaryCodeAnatomicalLocationBodySiteName;
    private String secondaryCodeAnatomicalLocationLateralityCode;
    private String secondaryCodeAnatomicalLocationLateralityTerminology;
    private String secondaryCodeAnatomicalLocationLateralityValue;
    private UUID secondaryCodeAnatomicalLocationUid;
    private LocalDateTime secondaryCodeClinicallyRelevantPeriodTimeOfOccurrence;
    private LocalDateTime secondaryCodeDeterminationDate;
    private String secondaryCodeSeverityTerminology;
    private String secondaryCodeSeverityCode;
    private String secondaryCodeSeverityValue;
    private String secondaryCodeLifePhaseBeginnCode;
    private String secondaryCodeLifePhaseBeginnValue;
    private String secondaryCodeLifePhaseBeginnTerminology;
    private String secondaryCodeLifePhaseEndeCode;
    private String secondaryCodeLifePhaseEndeTerminology;
    private String secondaryCodeLifePhaseEndeValue;
    private UUID secondaryCodeLifePhaseUid;
    private String secondaryCodeMultipleCodingIdentifierCode;
    private String secondaryCodeMultipleCodingIdentifierTerminology;
    private String secondaryCodeMultipleCodingIdentifierValue;
    private UUID secondaryCodeMultipleCodingIdentifierUid;
    private LocalDateTime secondaryCodeClinicallyRelevantPeriodTimeOfRecovery;
    private String secondaryCodeDiagnosticReliabilityTerminology;
    private String secondaryCodeDiagnosticReliabilityValue;
    private String secondaryCodeDiagnosticReliabilityCode;
    private String secondaryCodeDiagnosticExplanation;
    private String secondaryCodeClinicalStatusTerminology;
    private String secondaryCodeClinicalStatusCode;
    private String secondaryCodeClinicalStatusValue;
    private UUID secondaryCodeClinicalStatusUid;
    private LocalDateTime secondaryCodeLastDocumentationDate;
    private String secondaryCodeLanguageCode;
    private String secondaryCodeLanguageTerminology;
    private String secondaryCodeEncodingTerminology;
    private String secondaryCodeEncodingCode;

    // Diagnose
    private String languageTerminology;
    private String languageCode;
    private String territoryCode;
    private String territoryTerminology;
    private String composerName;
    private UUID uid;

}

