package org.ehrbase.fhirbridge.ehr.openehrclient;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestClient;
import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClientConfig;

import java.net.URI;
import java.util.Map;

class TestableDefaultRestClient extends DefaultRestClient {

    private final OpenEhrClientConfig config;

    public TestableDefaultRestClient(String baseUri) {
        super(null);
        this.config = new OpenEhrClientConfig(URI.create(baseUri));
    }

    @Override
    public OpenEhrClientConfig getConfig() {
        return config;
    }

    @Override
    public HttpResponse internalDelete(URI uri, Map<String, String> headers) {
        return super.internalDelete(uri, headers);
    }

    @Override
    protected HttpResponse internalPost(URI uri, Map<String, String> headers, String body, ContentType contentType, String accept) {
        return super.internalPost(uri, headers, body, contentType, accept);
    }
}

