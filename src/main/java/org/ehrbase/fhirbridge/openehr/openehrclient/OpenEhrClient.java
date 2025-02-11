package org.ehrbase.fhirbridge.openehr.openehrclient;

import java.util.UUID;

public interface OpenEhrClient {

    /**
     * Get the {@link EhrEndpoint}
     *
     * @return
     */
    EhrEndpoint ehrEndpoint();

    /**
     * Get the {@link CompositionEndpoint} for ehr with Id {@code ehrId}
     *
     * @param ehrId ehrId of ehr for which to revive compositions
     * @return
     */
    CompositionEndpoint compositionEndpoint(UUID ehrId);

    FolderDAO folder(UUID ehrId, String path);

    /**
     * Get the {@link TemplateEndpoint}
     *
     * @return
     */
    TemplateEndpoint templateEndpoint();

    /**
     * Get the {@link AqlEndpoint}
     *
     * @return
     */
    AqlEndpoint aqlEndpoint();

    /**
     * Get the {@link AdminEhrEndpoint}
     *
     * @return
     */
    AdminEhrEndpoint adminEhrEndpoint();

    AdminTemplateEndpoint adminTemplateEndpoint();

    /**
     * Get the {@link VersionedCompositionEndpoint}.
     *
     * @param ehrId the EHR identifier
     * @return the endpoint
     */
    VersionedCompositionEndpoint versionedCompositionEndpoint(UUID ehrId);
}
