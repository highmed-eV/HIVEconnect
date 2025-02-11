package org.ehrbase.fhirbridge.ehr.openehrclient;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.Locatable;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestClient;
import org.ehrbase.fhirbridge.openehr.openehrclient.OpenEhrClientConfig;
import org.ehrbase.fhirbridge.openehr.openehrclient.VersionUid;
import org.ehrbase.webtemplate.templateprovider.TemplateProvider;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

class TestableDefaultRestClient extends DefaultRestClient {

    private final OpenEhrClientConfig config;

    private final TemplateProvider templateProvider;

//    public TestableDefaultRestClient(OpenEhrClientConfig config) {
//        super(null);
//        this.config = config;
//    }

    public TestableDefaultRestClient(String baseUri) {
//        super(null);
//        this.config = new OpenEhrClientConfig(URI.create(baseUri));
////        this(new OpenEhrClientConfig(URI.create(baseUri)));
        this(baseUri, null);
    }

    public TestableDefaultRestClient(String baseUri, TemplateProvider templateProvider) {
        super(null);
        this.config = new OpenEhrClientConfig(URI.create(baseUri));
        this.templateProvider = templateProvider;
    }

    @Override
    public OpenEhrClientConfig getConfig() {
        return config;
    }

    public TemplateProvider getTemplateProvider() {
        return templateProvider;
    }

    @Override
    public VersionUid httpPost(URI uri, RMObject body) {
        return super.httpPost(uri, body);
    }

    @Override
    public VersionUid httpPut(URI uri, Locatable body, VersionUid versionUid) {
        return super.httpPut(uri, body, versionUid);
    }

    @Override
    public <T> Optional<T> httpGet(URI uri, Class<T> responseType) {
        return super.httpGet(uri, responseType);
    }

    @Override
    protected HttpResponse internalPost(URI uri, Map<String, String> headers, String body, ContentType contentType, String accept) {
        return super.internalPost(uri, headers, body, contentType, accept);
    }

    @Override
    public HttpResponse internalDelete(URI uri, Map<String, String> headers) {
        return super.internalDelete(uri, headers);
    }

    @Override
    protected HttpResponse internalGet(URI uri, Map<String, String> headers, String accept) {
        return super.internalGet(uri, headers, accept);
    }
}

