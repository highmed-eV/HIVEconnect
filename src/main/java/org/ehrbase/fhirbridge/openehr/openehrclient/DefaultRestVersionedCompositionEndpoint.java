package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.response.openehr.OriginalVersionResponseData;
import org.ehrbase.response.openehr.RevisionHistoryResponseData;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.fhirbridge.openehr.openehrclient.DefaultRestClient.OBJECT_MAPPER;
import static org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint.EHR_PATH;

@SuppressWarnings({"java:S6212", "java:S1075"})
public class DefaultRestVersionedCompositionEndpoint implements VersionedCompositionEndpoint {

    public static final String VERSIONED_COMPOSITION_PATH = "/versioned_composition/";

    public static final String REVISION_HISTORY_PATH = "/revision_history";

    public static final String VERSION_PATH = "/version";

    private final DefaultRestClient defaultRestClient;

    private final UUID ehrId;

    public DefaultRestVersionedCompositionEndpoint(DefaultRestClient defaultRestClient, UUID ehrId) {
        this.defaultRestClient = defaultRestClient;
        this.ehrId = ehrId;
    }

    @Override
    public Optional<VersionedComposition> find(UUID versionedObjectUid) {
        URI uri = defaultRestClient.getConfig()
                .getBaseUri()
                .resolve(EHR_PATH + ehrId + VERSIONED_COMPOSITION_PATH + versionedObjectUid);
        return defaultRestClient.httpGet(uri, VersionedComposition.class);
    }

    @Override
    public List<RevisionHistoryItem> findRevisionHistory(UUID versionedObjectUid) {
        URI uri = defaultRestClient.getConfig()
                .getBaseUri()
                .resolve(EHR_PATH + ehrId + VERSIONED_COMPOSITION_PATH + versionedObjectUid + REVISION_HISTORY_PATH);
        Optional<RevisionHistoryResponseData> result = defaultRestClient.httpGet(uri, RevisionHistoryResponseData.class);
        if (result.isEmpty()) {
            return new ArrayList<>();
        } else {
            return result.get().getRevisionHistoryItems();
        }
    }

    @Override
    public <T> Optional<OriginalVersion<T>> findVersionById(UUID versionedObjectUid, VersionUid versionUid, Class<T> clazz) {
        URI uri = defaultRestClient.getConfig()
                .getBaseUri()
                .resolve(EHR_PATH + ehrId + VERSIONED_COMPOSITION_PATH + versionedObjectUid + VERSION_PATH + "/" + versionUid.toString());

        return internalFindVersion(uri, clazz);
    }

    @Override
    public <T> Optional<OriginalVersion<T>> findVersionAtTime(UUID versionedObjectUid, @Nullable LocalDateTime versionAtTime, Class<T> clazz) {
        try {
            URIBuilder uriBuilder = new URIBuilder(defaultRestClient.getConfig()
                    .getBaseUri()
                    .resolve(EHR_PATH + ehrId + VERSIONED_COMPOSITION_PATH + versionedObjectUid + VERSION_PATH));

            if (versionAtTime != null) {
                uriBuilder.addParameter("version_at_time", versionAtTime.format(DateTimeFormatter.ISO_DATE_TIME));
            }

            return internalFindVersion(uriBuilder.build(), clazz);
        } catch (URISyntaxException e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    public <T> Optional<OriginalVersion<T>> internalFindVersion(URI uri, Class<T> clazz) {
        HttpResponse response = defaultRestClient.internalGet(uri, null, ContentType.APPLICATION_JSON.getMimeType());
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return Optional.empty();
        }

        try {
            TypeReference<OriginalVersionResponseData<Composition>> valueTypeRef = new TypeReference<>() {
            };
            return Optional
                    .of(OBJECT_MAPPER.readValue(response.getEntity().getContent(), valueTypeRef))
                    .map(originalVersion -> this.convert(originalVersion, clazz));
        } catch (IOException e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    /**
     * Converts an {@link OriginalVersionResponseData} into an {@link OriginalVersion}.
     *
     * @param originalVersion response data to convert
     * @param clazz           expected class
     * @param <T>             composition class
     * @return converted object
     */
    private <T> OriginalVersion<T> convert(OriginalVersionResponseData<Composition> originalVersion, Class<T> clazz) {
        OriginalVersion<T> result = new OriginalVersion<>();
        result.setUid(originalVersion.getVersionId());
        result.setPrecedingVersionUid(originalVersion.getPrecedingVersionUid());
        result.setLifecycleState(originalVersion.getLifecycleState());
        result.setCommitAudit(originalVersion.getAuditDetails());
        result.setSignature(originalVersion.getSignature());
        result.setOtherInputVersionUids(originalVersion.getOtherInputVersionUids());
        result.setAttestations(originalVersion.getAttestations());

        // T composition = new Flattener(defaultRestClient.getTemplateProvider())
        //         .flatten(originalVersion.getData(), clazz);
        T composition;
        try {
            composition = clazz.getDeclaredConstructor().newInstance();
            result.setData(composition);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }
}
