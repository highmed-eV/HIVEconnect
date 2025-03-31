// package org.ehrbase.fhirbridge.openehr.openehrclient;
package org.ehrbase.client.openehrclient.defaultrestclient;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.util.Optional;
import java.util.UUID;

import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestCompositionEndpoint;
import org.ehrbase.client.openehrclient.VersionUid;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;


import static org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint.EHR_PATH;

public class FHIRBridgeRestCompositionEndpoint extends DefaultRestCompositionEndpoint {
  public static final String COMPOSITION_PATH = "/composition/";
  private final DefaultRestClient defaultRestClient;
  private final UUID ehrId;
  
  public FHIRBridgeRestCompositionEndpoint(DefaultRestClient defaultRestClient, UUID ehrId) {
    super(defaultRestClient, ehrId);
    this.defaultRestClient = defaultRestClient;
    this.ehrId = ehrId;
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
    
    //update version
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

  // @Override
  public Composition mergeCanonicalCompositionEntity(Composition composition) {

    UIDBasedId compUIDBasedId = composition.getUid();

    Optional<VersionUid> versionUid = compUIDBasedId != null && compUIDBasedId.getValue() != null 
                                    ? Optional.of(new VersionUid(compUIDBasedId.getValue())) 
                                    : Optional.empty();

    final VersionUid updatedVersion = internalMerge(composition, versionUid.orElse(null));
    
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
 
}
