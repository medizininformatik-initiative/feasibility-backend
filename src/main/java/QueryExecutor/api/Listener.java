package QueryExecutor.api;

/**
 * Represents an entity capable of receiving results from a feasibility query running in a distributed fashion.
 */
public interface Listener {

    /**
     * Callback method to process feasibility query result updates.
     *
     * @param queryId  Identifies the query for which there is an update.
     * @param clientId Identifies the client within the query for which there is an update.
     * @param status   State of the query.
     */
    void onClientUpdate(String queryId, String clientId, QueryStatus status);
}
