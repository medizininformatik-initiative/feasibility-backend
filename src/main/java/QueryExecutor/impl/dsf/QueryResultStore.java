package QueryExecutor.impl.dsf;

import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.ClientNotFoundException;

import java.util.List;

/**
 * Represents a storage facility for {@link DSFQueryResult}s.
 */
public interface QueryResultStore {

    /**
     * Stores a result within the store.
     *
     * @param result The result that shall be stored.
     */
    void storeResult(DSFQueryResult result);

    /**
     * Gets the measure count from the store for a specific client associated with a query.
     *
     * @param queryId  Identifies the query that the measure count is related to.
     * @param clientId Identifies the client within the query that the measure count is related to.
     * @return The measure count.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws ClientNotFoundException If the given client ID does not identify a known client.
     */
    int getMeasureCount(String queryId, String clientId) throws QueryNotFoundException, ClientNotFoundException;

    /**
     * Gets all client IDs from the store that are associated with a specific query.
     * <p>
     * This will only return client IDs that already have a result (measure count) associated with them.
     *
     * @param queryId Identifies the query that the clients are related to.
     * @return List of all client IDs associated with the query that already have a result.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    List<String> getClientIdsWithResult(String queryId) throws QueryNotFoundException;

    /**
     * Removes a result from the store.
     *
     * @param queryId Identifies the query whose associated results shall be removed.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    void removeResult(String queryId) throws QueryNotFoundException;
}
