package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.client.annotations.Template;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.client.exception.WrongStatusCodeException;

import java.util.Optional;
import java.util.UUID;

public interface CompositionEndpoint {

    /**
     * Save a Flat-Entity to remote systems.
     *
     * @param entity Flat-Entity to save. Has to be annotated with {@link Template}
     * @return CompositionId
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    <T> T mergeCompositionEntity(T entity);

     /**
     * Save a Canonical-Entity to remote systems.
     *
     * @param entity Canonical-Entity to save.
     * @return CompositionId
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    String mergeCanonicalCompositionEntity(String entity);

    VersionUid mergeRaw(Composition composition);

    /**
     * Finds a Flat-Entity by
     *
     * @param compositionId CompositionId of the flat-Entity to retrieve.
     * @param clazz         class of the flat-Entity to retrieve. Has to be annotated with {@link Template}
     * @return The Flat-Entity
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    <T> Optional<T> find(UUID compositionId, Class<T> clazz);

    Optional<Composition> findRaw(UUID compositionId);

    /**
     * Deletes a Composition by preceding version uid.
     *
     * @param precedingVersionUid identifier of the Composition to be deleted.
     *                            This MUST be the last (most recent) version.
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    void delete(VersionUid precedingVersionUid);
}
