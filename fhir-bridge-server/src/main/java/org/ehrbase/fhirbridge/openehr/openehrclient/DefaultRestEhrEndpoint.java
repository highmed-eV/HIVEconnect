package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.nedap.archie.rm.ehr.EhrStatus;

import java.util.Optional;
import java.util.UUID;

public class DefaultRestEhrEndpoint implements EhrEndpoint {
    public static final String EHR_PATH = "rest/openehr/v1/ehr/";
    public static final String EHR_STATUS_PATH = "/ehr_status";
    private final DefaultRestClient defaultRestClient;

    public DefaultRestEhrEndpoint(DefaultRestClient defaultRestClient) {
        this.defaultRestClient = defaultRestClient;
    }

    @Override
    public UUID createEhr() {
        return defaultRestClient.httpPost(defaultRestClient.getConfig().getBaseUri().resolve(EHR_PATH), null).getUuid();
    }

    @Override
    public UUID createEhr(EhrStatus ehrStatus) {
        return defaultRestClient.httpPost(defaultRestClient.getConfig().getBaseUri().resolve(EHR_PATH), ehrStatus).getUuid();
    }


    @Override
    public Optional<EhrStatus> getEhrStatus(UUID ehrId) {
        return defaultRestClient.httpGet(defaultRestClient.getConfig().getBaseUri().resolve(EHR_PATH + ehrId.toString() + EHR_STATUS_PATH), EhrStatus.class);
    }

    @Override
    public void updateEhrStatus(UUID ehrId, EhrStatus ehrStatus) {
        defaultRestClient.httpPut(defaultRestClient.getConfig().getBaseUri().resolve(EHR_PATH + ehrId + EHR_STATUS_PATH), ehrStatus, new VersionUid(ehrStatus.getUid().getValue()));
    }
}
