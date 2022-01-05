package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.UnsupportedMediaTypeException;

import java.io.IOException;

/**
 * Represents an entity capable of managing different aspects of running distributed feasibility queries.
 */
interface QueryManager {

    /**
     * Creates a new query to be handled.
     *
     * @return The ID of the created query.
     */
    String createQuery();

    /**
     * Adds the necessary query definition to a handled query.
     * <p>
     * The query definition is added to a query identified by the given query ID.
     *
     * @param queryId   Identifies the query that the query definition shall be added to.
     * @param mediaType ?
     * @param content   The actual query in plain text.
     * @throws QueryNotFoundException        If the given query ID does not identify a known query.
     * @throws UnsupportedMediaTypeException If the given media type is not supported.
     */
    void addQueryDefinition(String queryId, String mediaType, String content) throws QueryNotFoundException, UnsupportedMediaTypeException;

    /**
     * Publishes a handled query for distributed execution.
     *
     * @param queryId Identifies the query that shall be published.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws IOException If the query identified by the given query ID can not be published.
     */
    void publishQuery(String queryId) throws QueryNotFoundException, IOException;

    /**
     * Removes a query from being handled if there is any.
     *
     * @param queryId Identifies the query that shall be removed.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    void removeQuery(String queryId) throws QueryNotFoundException;
}
