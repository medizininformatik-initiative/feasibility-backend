package de.numcodex.feasibility_gui_backend.service;

import QueryBuilderMoc.QueryBuilder;
import de.numcodex.feasibility_gui_backend.model.query.QueryDefinition;
import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.ResultLocation;
import org.springframework.stereotype.Service;

@Service
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
