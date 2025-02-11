package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VersionUidTest {

    @Test
    void constructorWithVersionString() {
        String versionString = "123e4567-e89b-12d3-a456-426614174000::system::42";

        VersionUid versionUid = new VersionUid(versionString);

        assertNotNull(versionUid);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), versionUid.getUuid());
        assertEquals("system", versionUid.getSystem());
        assertEquals(42L, versionUid.getVersion());
    }

    @Test
    void constructorWithVersionString_NoSystemAndVersion() {
        String versionString = "123e4567-e89b-12d3-a456-426614174000";

        VersionUid versionUid = new VersionUid(versionString);

        assertNotNull(versionUid);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), versionUid.getUuid());
        assertEquals("", versionUid.getSystem());
        assertEquals(1L, versionUid.getVersion());
    }

    @Test
    void constructorWithUuidSystemAndVersion() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String system = "system";
        long version = 42L;

        VersionUid versionUid = new VersionUid(uuid, system, version);

        assertNotNull(versionUid);
        assertEquals(uuid, versionUid.getUuid());
        assertEquals(system, versionUid.getSystem());
        assertEquals(version, versionUid.getVersion());
    }

    @Test
    void testToString() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String system = "system";
        long version = 42L;

        VersionUid versionUid = new VersionUid(uuid, system, version);

        assertEquals("123e4567-e89b-12d3-a456-426614174000::system::42", versionUid.toString());
    }
}

