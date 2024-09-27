package org.ehrbase.fhirbridge.engine.converter.specific.kdsdiagnose;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ehrbase.fhirbridge.engine.converter.generic.ConditionToCompositionConverter;
import org.ehrbase.fhirbridge.engine.opt.kdsdiagnosecomposition.KDSDiagnoseComposition;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.lang.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class KDSDiagnoseCompositionConverter extends ConditionToCompositionConverter<KDSDiagnoseComposition> {

    @Override
    public KDSDiagnoseComposition convertInternal(@NonNull Condition resource) {
        KDSDiagnoseComposition composition = new KDSDiagnoseComposition();
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("flat_json/kds_diagnose_flat.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found in resources");
            }
            composition = objectMapper.readValue(inputStream, KDSDiagnoseComposition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return composition;
    }
}
