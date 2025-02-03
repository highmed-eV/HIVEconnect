package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.ehrbase.client.annotations.Id;
import org.ehrbase.client.exception.ClientException;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint.EHR_PATH;

public class DefaultRestCompositionEndpoint implements CompositionEndpoint {
  public static final String COMPOSITION_PATH = "/composition/";
  private final DefaultRestClient defaultRestClient;
  private final UUID ehrId;

  public DefaultRestCompositionEndpoint(DefaultRestClient defaultRestClient, UUID ehrId) {
    this.defaultRestClient = defaultRestClient;
    this.ehrId = ehrId;
  }

  public static Optional<VersionUid> extractVersionUid(Object entity) {
    return Arrays.stream(FieldUtils.getAllFields(entity.getClass()))
        .filter(f -> f.isAnnotationPresent(Id.class))
        .findAny()
        .map(
            idField -> {
              try {
                PropertyDescriptor propertyDescriptor =
                    new PropertyDescriptor(idField.getName(), entity.getClass());
                return (VersionUid) propertyDescriptor.getReadMethod().invoke(entity);
              } catch (IllegalAccessException
                  | InvocationTargetException
                  | IntrospectionException e) {
                throw new ClientException(e.getMessage(), e);
              }
            });
  }

  @Override
  public <T> T mergeCompositionEntity(T entity) {
    Composition composition =
        (Composition) entity;
            // new Unflattener(
            //         defaultRestClient.getTemplateProvider(),
            //         defaultRestClient.getDefaultValuesProvider())
            //     .unflatten(entity);

    Optional<VersionUid> versionUid = extractVersionUid(entity);

    final VersionUid updatedVersion = internalMerge(composition, versionUid.orElse(null));
    
    //TODO: update version
    if (composition.getUid() == null) {
      UIDBasedId uIDBasedId = new ObjectVersionId(updatedVersion.toString());
      composition.setUid(uIDBasedId);
    } else {
      //update with the incremented versionedUID
      composition.getUid().setValue(updatedVersion.toString());
    }

    // Flattener.addVersion(entity, updatedVersion);
    // entity =
    //     (T)
    //         new Flattener(defaultRestClient.getTemplateProvider())
    //             .flatten(composition, entity.getClass());
    // Flattener.addVersion(entity, updatedVersion);

    return entity;
  }

  @Override
  public Composition mergeCanonicalCompositionEntity(Composition composition) {

    UIDBasedId compUIDBasedId = composition.getUid();

    Optional<VersionUid> versionUid = compUIDBasedId != null && compUIDBasedId.getValue() != null 
                                    ? Optional.of(new VersionUid(compUIDBasedId.getValue())) 
                                    : Optional.empty();

    final VersionUid updatedVersion = internalMerge(composition, versionUid.orElse(null));
    
    //TODO: update version
    if (composition.getUid() == null) {
      UIDBasedId uIDBasedId = new ObjectVersionId(updatedVersion.toString());
      composition.setUid(uIDBasedId);
    } else {
      //update with the incremented versionedUID
      composition.getUid().setValue(updatedVersion.toString());
    }

    return composition;
  }

  private VersionUid internalMerge(Composition composition, VersionUid versionUid) {
    final VersionUid updatedVersion;
    if (versionUid == null) {
      updatedVersion =
          defaultRestClient.httpPost(
              defaultRestClient
                  .getConfig()
                  .getBaseUri()
                  .resolve(EHR_PATH + ehrId.toString() + COMPOSITION_PATH),
                  composition);
    } else {
      updatedVersion =
          defaultRestClient.httpPut(
              defaultRestClient
                  .getConfig()
                  .getBaseUri()
                  .resolve(
                      EHR_PATH + ehrId.toString() + COMPOSITION_PATH + versionUid.getUuid()),
                  composition,
              versionUid);
    }
    return updatedVersion;
  }

  @Override
  public VersionUid mergeRaw(Composition composition) {

    Optional<VersionUid> versionUid = Optional.ofNullable(composition.getUid()).map(ObjectId::toString).map(VersionUid::new);

    VersionUid newVersionUid = internalMerge(composition, versionUid.orElse(null));

    return newVersionUid;
  }

  @Override
  public <T> Optional<T> find(UUID compositionId, Class<T> clazz) {
    Optional<Composition> composition = findRaw(compositionId);

    // return composition.map(
        // c -> new Flattener(defaultRestClient.getTemplateProvider()).flatten(c, clazz));
    return (Optional<T>) composition;
  }
  
  @Override
  public Optional<Composition> findRaw(UUID compositionId) {
    Optional<Composition> composition =
        defaultRestClient.httpGet(
            defaultRestClient
                .getConfig()
                .getBaseUri()
                .resolve(EHR_PATH + ehrId.toString() + COMPOSITION_PATH + compositionId.toString()),
            Composition.class);
    return composition;
  }


  @Override
  public void delete(VersionUid precedingVersionUid) {
    if (precedingVersionUid == null) {
      throw new ClientException("precedingVersionUid mush not be null");
    }

    URI uri = defaultRestClient.getConfig()
            .getBaseUri()
            .resolve(EHR_PATH + ehrId.toString() + COMPOSITION_PATH + precedingVersionUid);
    defaultRestClient.internalDelete(uri, new HashMap<>());
  }
}
