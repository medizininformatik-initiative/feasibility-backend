package QueryExecutor.api;

import java.util.List;

/**
 * Represents a client for creating and triggering execution of distributed feasibility queries as well as
 * collecting and providing their results.
 */
public interface BrokerClient {

    /**
     * Adds a listener that gets called whenever a feasibility query returns with a result.
     *
     * @param listener The listener that shall be called.
     */
    void addListener(Listener listener);

    /**
     * Creates a new unpublished query.
     *
     * @return The ID of the created query.
     */
    String createQuery();

    /**
     * Adds the necessary query definition to a query.
     * <p>
     * The query definition is added to a query identified by the given query ID.
     *
     * @param queryId   Identifies the query that the query definition shall be added to.
     * @param mediaType ?
     * @param content   The actual query in plain text.
     * @throws QueryNotFoundException        If the given query ID does not identify a known query.
     * @throws UnsupportedMediaTypeException If the given media type is not supported.
     */
    void addQueryDefinition(String queryId, String mediaType, String content) throws QueryNotFoundException,
            UnsupportedMediaTypeException;

    /**
     * Publishes a query for distributed execution.
     *
     * @param queryId Identifies the query that shall be published.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws PublishFailedException If the query can not be published.
     */
    void publishQuery(String queryId) throws QueryNotFoundException, PublishFailedException;

    /**
     * Marks an already published query as closed.
     * <p>
     * After calling this function calls to {@link #getResultFeasibility(String, String)} and
     * {@link #getResultClientIds(String)} will fail.
     *
     * @param queryId Identifies the query that shall be closed.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    void closeQuery(String queryId) throws QueryNotFoundException;

    /**
     * Gets the feasibility (measure count) of a published query for a specific client.
     *
     * @param queryId  Identifies the query.
     * @param clientId Identifies the client within the query whose feasibility shall be gotten.
     * @return The feasibility for a specific client within a query.
     * @throws QueryNotFoundException  If the given query ID does not identify a known query.
     * @throws ClientNotFoundException If the given client ID does not identify a known client within a query.
     */
    int getResultFeasibility(String queryId, String clientId) throws QueryNotFoundException, ClientNotFoundException;

    /**
     * Gets all client IDs associated with a published query that can already provide a result.
     *
     * @param queryId Identifies the query whose associated client IDs with results shall be gotten.
     * @return All client IDs of a specific query that can already provide results.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    List<String> getResultClientIds(String queryId) throws QueryNotFoundException;
}
