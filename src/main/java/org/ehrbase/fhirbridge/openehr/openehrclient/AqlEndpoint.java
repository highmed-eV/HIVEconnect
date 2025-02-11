package org.ehrbase.fhirbridge.openehr.openehrclient;

import java.util.List;
import org.ehrbase.client.aql.parameter.ParameterValue;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record;
import org.ehrbase.response.openehr.QueryResponseData;

public interface AqlEndpoint {

  <T extends Record> List<T> execute(Query<T> query, ParameterValue... parameterValues);

  QueryResponseData executeRaw(Query query, ParameterValue... parameterValues);
}
