package org.ehrbase.fhirbridge.engine.opt.kdsdiagnosecomposition.definition;

import org.ehrbase.client.annotations.Entity;
import org.ehrbase.client.annotations.OptionFor;
import org.ehrbase.client.annotations.Path;
import org.ehrbase.client.classgenerator.interfaces.RMEntity;

import javax.annotation.processing.Generated;

@Entity
@Generated(
    value = "org.ehrbase.client.classgenerator.ClassGenerator",
    date = "2023-11-22T15:55:37.607430679+01:00",
    comments = "https://github.com/ehrbase/openEHR_SDK Version: 1.25.0"
)
@OptionFor("DV_CODED_TEXT")
public class MehrfachkodierungskennzeichenIcd10GmMehrfachkodierungkennzeichenDvCodedText implements RMEntity, MehrfachkodierungskennzeichenIcd10GmMehrfachkodierungkennzeichenChoice {
  /**
   * Path: Diagnose/Primärcode/Mehrfachkodierungskennzeichen_ICD-10-GM/Mehrfachkodierungkennzeichen/Mehrfachkodierungkennzeichen
   * Description: ICD-10 GM Zusatzkennzeichen nach Kreuz-Stern-System.
   */
  @Path("|defining_code")
  private MehrfachkodierungkennzeichenDefiningCode mehrfachkodierungkennzeichenDefiningCode;

  public void setMehrfachkodierungkennzeichenDefiningCode(
      MehrfachkodierungkennzeichenDefiningCode mehrfachkodierungkennzeichenDefiningCode) {
     this.mehrfachkodierungkennzeichenDefiningCode = mehrfachkodierungkennzeichenDefiningCode;
  }

  public MehrfachkodierungkennzeichenDefiningCode getMehrfachkodierungkennzeichenDefiningCode() {
     return this.mehrfachkodierungkennzeichenDefiningCode ;
  }
}
