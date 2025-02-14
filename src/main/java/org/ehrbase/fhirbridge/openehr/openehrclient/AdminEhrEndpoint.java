package org.ehrbase.fhirbridge.openehr.openehrclient;

import java.util.UUID;

public interface AdminEhrEndpoint {
  int delete(UUID ehrId);
}
