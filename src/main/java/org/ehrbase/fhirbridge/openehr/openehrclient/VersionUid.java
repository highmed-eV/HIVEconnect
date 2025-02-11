package org.ehrbase.fhirbridge.openehr.openehrclient;

import java.util.UUID;

public class VersionUid {
    private final UUID uuid;
    private final String system;
    private final long version;


    public VersionUid(String versionString) {
        String[] split = versionString.split("::");
        uuid = UUID.fromString(split[0]);
        if (split.length > 1) {
            system = split[1];
            version = Long.parseLong(split[2]);
        } else {
            system = "";
            version = 1L;
        }
    }

    public VersionUid(UUID uuid, String system, long version) {
        this.uuid = uuid;
        this.system = system;
        this.version = version;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getSystem() {
        return system;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return uuid.toString() + "::" + system + "::" + version;
    }
}
