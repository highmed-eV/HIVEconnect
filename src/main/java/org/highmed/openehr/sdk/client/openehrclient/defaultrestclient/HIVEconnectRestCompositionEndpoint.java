package org.highmed.openehr.sdk.client.openehrclient.defaultrestclient;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestCompositionEndpoint;

import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint.EHR_PATH;

public class HIVEconnectRestCompositionEndpoint extends DefaultRestCompositionEndpoint {
  private static final String COMPOSITION_PATH = "composition/";
  private final DefaultRestClient defaultRestClient;
  private final UUID ehrId;
  
  public HIVEconnectRestCompositionEndpoint(DefaultRestClient defaultRestClient, UUID ehrId) {
    super(defaultRestClient, ehrId);
    this.defaultRestClient = defaultRestClient;
    this.ehrId = ehrId;
  }

  
  @Override
  public <T> T mergeCompositionEntity(T entity) {
    Composition composition =
        (Composition) entity;

    Optional<ObjectVersionId> objectVersionId = extractVersionUid(entity);

    final ObjectVersionId updatedVersion = refactoredInternalMerge(composition, objectVersionId.orElse(null));
    
    //update version
    if (composition.getUid() == null) {
      UIDBasedId uIDBasedId = new ObjectVersionId(updatedVersion.toString());
      composition.setUid(uIDBasedId);
    } else {
      //update with the incremented versionedUID
      composition.getUid().setValue(updatedVersion.toString());
    }

    return entity;
  }

  // @Override
  public Composition mergeCanonicalCompositionEntity(Composition composition) {

    UIDBasedId compUIDBasedId = composition.getUid();

    Optional<ObjectVersionId> objectVersionId = compUIDBasedId != null && compUIDBasedId.getValue() != null
                                    ? Optional.of(new ObjectVersionId(compUIDBasedId.getValue())) 
                                    : Optional.empty();

    final ObjectVersionId updatedVersion = refactoredInternalMerge(composition, objectVersionId.orElse(null));
    
    //update version
    if (composition.getUid() == null) {
      UIDBasedId uIDBasedId = new ObjectVersionId(updatedVersion.toString());
      composition.setUid(uIDBasedId);
    } else {
      //update with the incremented versionedUID
      composition.getUid().setValue(updatedVersion.toString());
    }

    return composition;
  }


  private ObjectVersionId refactoredInternalMerge(Composition composition, ObjectVersionId objectVersionId) {
    final ObjectVersionId updatedVersion;
    if (objectVersionId == null) {
      updatedVersion =
          defaultRestClient.httpPost(
              defaultRestClient
                  .getConfig()
                  .getBaseUri()
                  .resolve(EHR_PATH + ehrId.toString() + "/"+COMPOSITION_PATH),
                  composition);
    } else {
      updatedVersion =
          defaultRestClient.httpPut(
              defaultRestClient
                  .getConfig()
                  .getBaseUri()
                  .resolve(
                      EHR_PATH + ehrId.toString() + "/" + COMPOSITION_PATH + objectVersionId.getValue()),
                  composition,
              objectVersionId);
    }
    return updatedVersion;
  }
 
}
