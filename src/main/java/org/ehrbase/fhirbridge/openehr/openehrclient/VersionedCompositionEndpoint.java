package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistoryItem;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersionedCompositionEndpoint {

    /**
     * Retrieves the versioned composition identified by <code>versioned_object_uid</code>.
     *
     * @param versionedObjectUid identifier of the  versioned composition
     * @return the versioned composition, or an empty {@link Optional}
     */
    Optional<VersionedComposition> find(UUID versionedObjectUid);

    /**
     * Retrieves the list of revision history items for a given versioned composition.
     *
     * @param versionedObjectUid identifier of the  versioned composition
     * @return list of revision history items, or an empty {@link List}
     */
    List<RevisionHistoryItem> findRevisionHistory(UUID versionedObjectUid);

    /**
     * Retrieves a version identified by <code>version_uid</code> for a given versioned composition.
     *
     * @param versionedObjectUid identifier of the versioned composition
     * @param versionUid         identifier of the version
     * @param clazz              expected class
     * @param <T>                expected type
     * @return the version of the versioned composition, or an empty {@link Optional}
     */
    <T> Optional<OriginalVersion<T>> findVersionById(UUID versionedObjectUid, VersionUid versionUid, Class<T> clazz);

    /**
     * Retrieves a version for a given versioned composition.
     * <p>
     * If <code>version_at_time</code> is supplied, retrieves the version extant at specified time,
     * otherwise retrieves the latest version.
     *
     * @param versionedObjectUid identifier of the versioned composition
     * @param versionAtTime      given time in the extended ISO 8601 format
     * @param clazz              expected class
     * @param <T>                expected type
     * @return the version of the versioned composition, or an empty {@link Optional}
     */
    <T> Optional<OriginalVersion<T>> findVersionAtTime(UUID versionedObjectUid, @Nullable LocalDateTime versionAtTime, Class<T> clazz);
}
