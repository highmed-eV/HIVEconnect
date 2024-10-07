package org.ehrbase.fhirbridge.engine.converter;

import java.util.HashMap;
import java.util.Map;

public class ProfileToCanonicalMapper {
    //Temp class. This will be replaced by the engine
    private static final Map<String, String> profileToCanonicalMap = new HashMap<>();

    static {
        // Initialize the map with resource type to canonical JSON mappings
        profileToCanonicalMap.put("Patient", "GECCO_Personendaten_canonical.json");
        profileToCanonicalMap.put("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-gas-panel", "kds_diagnose_canonical.json");
        
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose", "kds_diagnose_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ServiceRequestLab", "kds_laborauftrag_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/DiagnosticReportLab", "kds_laborbericht_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-medikation/StructureDefinition/MedicationAdministration", "KDS_Medikamentenverabreichungen_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-medikation/StructureDefinition/medikationsliste", "kds_medikationseintrag_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Patient", "kds_person_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-prozedur/StructureDefinition/Procedure", "kds_prozedur_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-fall/StructureDefinition/KontaktGesundheitseinrichtung", "stationarer_versorgungsfall_canonical.json");
        profileToCanonicalMap.put("https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/ResearchSubject", "studienteilnahme_canonical.json");
        // Add more mappings as needed
    }

    public static String getProfileToCanonicalMap(String resourceId){
        // Method to get the canonical JSON file for a given resource type
        return profileToCanonicalMap.get(resourceId);
    }
}
