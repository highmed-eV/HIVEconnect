package org.ehrbase.fhirbridge.camel.component.ehr.composition;

/**
 * Operations supported by the {@link org.ehrbase.client.openehrclient.CompositionEndpoint}
 */
@SuppressWarnings("java:S115")
public enum CompositionOperation {

    /**
     * Save a Flat-Entity to remote systems.
     */
    mergeCompositionEntity,

    /**
     * Save a Canonocal-Entity to remote systems.
     */
    mergeCanonicalCompositionEntity,

    /**
     * Finds a Flat-Entity by CompositionId.
     */
    find
}
