package org.ehrbase.fhirbridge.engine.converter;

import java.util.HashMap;
import java.util.Map;

public class ResourceToCanonicalMapper {
    //Temp class. This will be replaced by the engine
    private static final Map<String, String> resourceToCanonicalMap = new HashMap<>();

    static {
        // Initialize the map with resource type to canonical JSON mappings
        resourceToCanonicalMap.put("Patient", "GECCO_Personendaten_canonical.json");
        resourceToCanonicalMap.put("kds_diagnose_bundle", "kds_diagnose_canonical.json");
        resourceToCanonicalMap.put("kds_laborauftrag_bundle", "kds_laborauftrag_canonical.json");
        resourceToCanonicalMap.put("kds_laborbericht_bundle", "kds_laborbericht_canonical.json");
        resourceToCanonicalMap.put("KDS_Medikamentenverabreichungen_Bundle", "KDS_Medikamentenverabreichungen_canonical.json");
        resourceToCanonicalMap.put("kds_medikationseintrag_bundle", "kds_medikationseintrag_canonical.json");
        resourceToCanonicalMap.put("kds_person_bundle", "kds_person_canonical.json");
        resourceToCanonicalMap.put("kds_prozedur_bundle", "kds_prozedur_canonical.json");
        resourceToCanonicalMap.put("stationarer_versorgungsfall_bundle", "stationarer_versorgungsfall_canonical.json");
        resourceToCanonicalMap.put("studienteilnahme_bundle", "studienteilnahme_canonical.json");
        // Add more mappings as needed
    }

    public static String getResourceToCanonicalMap(String resourceId){
        // Method to get the canonical JSON file for a given resource type
        return resourceToCanonicalMap.get(resourceId);
    }
}
