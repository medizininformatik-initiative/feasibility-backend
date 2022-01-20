package de.numcodex.feasibility_gui_backend.query.broker;

import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType;

import java.io.IOException;
import java.util.List;

/**
 * Represents a client for creating and triggering execution of distributed feasibility queries as well as
 * collecting and providing their results.
 */
public interface BrokerClient {

    /**
     * Gets the type of the broker.
     *
     * @return The broker type.
     */
    BrokerClientType getBrokerType();

    /**
     * Adds a listener that gets called whenever a feasibility query changes its status.
     *
     * @param queryStatusListener The listener that shall be called.
     * @throws IOException IO/communication error
     */
    void addQueryStatusListener(QueryStatusListener queryStatusListener) throws IOException;

    /**
     * Creates a new unpublished query.
     *
     * @return The ID of the created query.
     * @throws IOException IO/communication error
     */
    String createQuery() throws IOException;

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
     * @throws IOException                   IO/communication error
     */
    void addQueryDefinition(String queryId, String mediaType, String content) throws QueryNotFoundException,
            UnsupportedMediaTypeException, IOException;

    /**
     * Publishes a query for distributed execution.
     *
     * @param queryId Identifies the query that shall be published.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws IOException            If the query was not published due IO/communication error
     */
    void publishQuery(String queryId) throws QueryNotFoundException, IOException;

    /**
     * Marks an already published query as closed.
     * <p>
     * After calling this function calls to {@link #getResultFeasibility(String, String)} and
     * {@link #getResultSiteIds(String)} will fail.
     *
     * @param queryId Identifies the query that shall be closed.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws IOException            IO/communication error
     */
    void closeQuery(String queryId) throws QueryNotFoundException, IOException;

    /**
     * Gets the feasibility (measure count) of a published query for a specific site.
     *
     * @param queryId Identifies the query.
     * @param siteId  Identifies the site within the query whose feasibility shall be gotten.
     * @return The feasibility for a specific site within a query.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws SiteNotFoundException  If the given site ID does not identify a known site within a query.
     * @throws IOException            IO/communication error
     */
    int getResultFeasibility(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException, IOException;

    /**
     * Gets all site IDs associated with a published query that can already provide a result.
     *
     * @param queryId Identifies the query whose associated site IDs with results shall be gotten.
     * @return All site IDs of a specific query that can already provide results.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws IOException            IO/communication error
     */
    List<String> getResultSiteIds(String queryId) throws QueryNotFoundException, IOException;

    /**
     * Gets the display name of a site.
     *
     * @param siteId Identifies the site whose display name shall be gotten.
     * @return The display name of the site.
     * @throws SiteNotFoundException If the given site ID does not identify a known site.
     * @throws IOException           IO/communication error
     */
    String getSiteName(String siteId) throws SiteNotFoundException, IOException;
}
