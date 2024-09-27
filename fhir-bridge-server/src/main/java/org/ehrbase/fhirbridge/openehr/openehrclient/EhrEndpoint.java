package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.nedap.archie.rm.ehr.EhrStatus;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.client.exception.WrongStatusCodeException;

import java.util.Optional;
import java.util.UUID;

public interface EhrEndpoint {

    /**
     * Create a new EHR.
     *
     * @return ehrID
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    UUID createEhr();

    /**
     * Create a new EHR with the given EHR_STATUS.
     *
     * @param ehrStatus EHR_STATUS object to create the EHR with.
     * @return ehrID
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    UUID createEhr(EhrStatus ehrStatus);

    /**
     * Get the EhrStatus for {@code ehrId}.
     *
     * @param ehrId Id of the ehr from which to return the status.
     * @return {@link EhrStatus}
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    Optional<EhrStatus> getEhrStatus(UUID ehrId);

    /**
     * Updates the status of the ehr with  {@code ehrId} to {@code ehrStatus}
     *
     * @param ehrId     EhrId of the ehr which will be updated
     * @param ehrStatus new ehrStatus
     * @throws ClientException
     * @throws WrongStatusCodeException
     */
    void updateEhrStatus(UUID ehrId, EhrStatus ehrStatus);
}
