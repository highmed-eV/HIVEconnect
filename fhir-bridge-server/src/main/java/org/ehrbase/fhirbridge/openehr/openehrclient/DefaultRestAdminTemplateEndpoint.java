package org.ehrbase.fhirbridge.openehr.openehrclient;

import org.apache.http.HttpResponse;

import java.net.URI;

/**
 * Requires that the Admin endpoint is active.
 *
 * Parameters in .yml configuration matching the runtime context as:
 *
 * admin-api:
 *   active: true
 *   allowDeleteAll: true
 */
public class DefaultRestAdminTemplateEndpoint implements AdminTemplateEndpoint {

    public static final String ADMIN_TEMPLATE_PATH = "rest/admin/template/";

    private final DefaultRestClient defaultRestClient;

    public DefaultRestAdminTemplateEndpoint(DefaultRestClient defaultRestClient) {
        this.defaultRestClient = defaultRestClient;
    }


    @Override
    public int delete(String templateId) {
        if (templateId == null) {
            return 0;
        }

        URI uri = defaultRestClient.getConfig().getBaseUri().resolve(ADMIN_TEMPLATE_PATH + templateId);

        HttpResponse response =
                defaultRestClient.internalDelete(
                        uri,
                        null);

        return response.getStatusLine().getStatusCode();
    }
}
