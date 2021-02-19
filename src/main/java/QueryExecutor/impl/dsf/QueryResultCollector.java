package QueryExecutor.impl.dsf;

import QueryExecutor.api.Listener;
import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.ClientNotFoundException;

import java.util.List;

/**
 * Represents an entity capable of collecting query results and notifying interested parties about them.
 */
interface QueryResultCollector {

    /**
     * Registers a listener that gets called whenever a new query result comes in.
     *
     * @param listener The listener that gets notified for new results.
     */
    void addResultListener(Listener listener);

    /**
     * Gets the feasibility of a specific client within a query.
     *
     * @param queryId  Identifies the associated query.
     * @param clientId Identifies the client within the query.
     * @return Client's feasibility within the query.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws ClientNotFoundException If the given client ID does not identify a known client within the query results.
     */
    int getResultFeasibility(String queryId, String clientId) throws QueryNotFoundException, ClientNotFoundException;

    /**
     * Gets all client identifiers of a specific query who already published a query result.
     *
     * @param queryId Identifies the associated query.
     * @return All client identifiers who already published a query result.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    List<String> getResultClientIds(String queryId) throws QueryNotFoundException;

    /**
     * Removes all results associated with a specific query if there are any.
     *
     * @param queryId Identifies the associated query.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    void removeResults(String queryId) throws QueryNotFoundException;
}
