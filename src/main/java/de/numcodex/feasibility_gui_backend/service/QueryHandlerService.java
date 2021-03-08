package de.numcodex.feasibility_gui_backend.service;

import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class QueryHandlerService {

    public String runQuery(StructuredQuery query) {
        // Create QueryMessage (QueryMetadata + Query)
        // Send to QueryEndpoint/QueryBuilder

        // TODO: call sequence
        // id = createQuery()
        // fhir/cql
        // addQueryDefinition(id, mt, content)
        // publishQuery()
        return null;
    }

    public QueryResult getQueryResult(String resultLocation) {
        // Request QueryResult
        // Optional: Translate for UI
        // Forward to UI
        return null;
    }

    public String getQueryContent(QueryBuilder queryBuilder) {
        // TODO: adjust to restructuring
        return queryBuilder.getQueryContent(null);
    }
}
