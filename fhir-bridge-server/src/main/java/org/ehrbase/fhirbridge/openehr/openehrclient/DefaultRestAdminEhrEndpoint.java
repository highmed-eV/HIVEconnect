package org.ehrbase.fhirbridge.openehr.openehrclient;

import org.apache.http.HttpResponse;

import java.net.URI;
import java.util.UUID;

/**
 * Requires that the Admin endpoint is active.
 *
 * Parameters in .yml configuration matching the runtime context as:
 *
 * admin-api:
 *   active: true
 *   allowDeleteAll: true
 */
public class DefaultRestAdminEhrEndpoint implements AdminEhrEndpoint {

    public static final String ADMIN_EHR_PATH = "rest/admin/ehr/";

    private final DefaultRestClient defaultRestClient;

    public DefaultRestAdminEhrEndpoint(DefaultRestClient defaultRestClient) {
        this.defaultRestClient = defaultRestClient;
    }


    @Override
    public int delete(UUID ehrId) {
        if (ehrId == null) {
            return 0;
        }

        URI uri = defaultRestClient.getConfig().getBaseUri().resolve(ADMIN_EHR_PATH + ehrId.toString());

        HttpResponse response =
                defaultRestClient.internalDelete(
                        uri,
                        null);

        return response.getStatusLine().getStatusCode();
    }
}
