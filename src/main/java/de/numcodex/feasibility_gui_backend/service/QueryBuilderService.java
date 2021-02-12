package de.numcodex.feasibility_gui_backend.service;

import QueryBuilderMoc.QueryBuilder;
import de.numcodex.feasibility_gui_backend.model.QueryDefinition;
import de.numcodex.feasibility_gui_backend.model.QueryResult;
import de.numcodex.feasibility_gui_backend.model.ResultLocation;

public class QueryBuilderService {

  public ResultLocation runQuery(QueryDefinition query) {
    // Create QueryMessage (QueryMetadata + Query)
    // Send to QueryEndpoint/QueryBuilder
    return null;
  }

  public QueryResult getQueryResult(ResultLocation resultLocation) {
    // Request QueryResult
    // Optional: Translate for UI
    // Forward to UI
    return null;
  }

  public String getQueryContent(QueryBuilder queryBuilder) {
    return queryBuilder.getQueryContent();
  }
}
