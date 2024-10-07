package org.ehrbase.fhirbridge.fhir.support;

import org.ehrbase.fhirbridge.camel.processor.PatientReferenceProcessor;
import org.ehrbase.fhirbridge.fhir.common.Profile;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Bundles {
    private static final Logger log = LoggerFactory.getLogger(Bundles.class);

    private Bundles() {
    }

    public static Optional<Profile> getTransactionProfile(Bundle bundle) {
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            //return the default bundle
            Profile profile = Profile.ENCOUNTER_DEFAULT;
            return Optional.of(profile);
            
            // if (Resources.hasProfile(entry.getResource(), Profile.BLOOD_GAS_PANEL)) {
            //     return Optional.of(Profile.BLOOD_GAS_PANEL);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.ANTI_BODY_PANEL)) {
            //     return Optional.of(Profile.ANTI_BODY_PANEL);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.DIAGNOSTIC_REPORT_LAB)) {
            //     return Optional.of(Profile.DIAGNOSTIC_REPORT_LAB);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.VIROLOGISCHER_BEFUND)) {
            //     return Optional.of(Profile.VIROLOGISCHER_BEFUND);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.UCC_SENSORDATEN_STEPS)) {
            //     return Optional.of(Profile.UCC_SENSORDATEN_STEPS);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.UCC_SENSORDATEN_VITALSIGNS)) {
            //     return Optional.of(Profile.UCC_SENSORDATEN_VITALSIGNS);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.UCC_SENSORDATEN_VITALSIGNS)) {
            //     return Optional.of(Profile.UCC_SENSORDATEN_VITALSIGNS);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.ITI65)) {
            //     return Optional.of(Profile.ITI65);
            // }
            // if (Resources.hasProfile(entry.getResource(), Profile.UCC_APP_PRO_DATEN)) {
            //     return Optional.of(Profile.UCC_APP_PRO_DATEN);
            // }
                
            // if (entry.getResource().getChildByName("subject") != null) {
            //     Profile profile = Profile.ENCOUNTER_DEFAULT;
            //     String firstProfile = entry.getResource().getMeta().getProfile().get(0).getValueAsString();
            //     profile.setDefaultProfileUrl(firstProfile);
            //     log.info("#########Default profile: " + firstProfile);
            //     return Optional.of(profile);
            // }
        }
        return Optional.empty();
    }
}
