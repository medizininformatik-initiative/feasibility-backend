package de.numcodex.feasibility_gui_backend.service.query_executor;

import java.util.List;

/**
 * Represents a client for creating and triggering execution of distributed feasibility queries as well as
 * collecting and providing their results.
 */
public interface BrokerClient {

    /**
     * Adds a listener that gets called whenever a feasibility query changes its status.
     *
     * @param queryStatusListener The listener that shall be called.
     */
    void addQueryStatusListener(QueryStatusListener queryStatusListener);

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
     * {@link #getResultSiteIds(String)} will fail.
     *
     * @param queryId Identifies the query that shall be closed.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    void closeQuery(String queryId) throws QueryNotFoundException;

    /**
     * Gets the feasibility (measure count) of a published query for a specific site.
     *
     * @param queryId Identifies the query.
     * @param siteId  Identifies the site within the query whose feasibility shall be gotten.
     * @return The feasibility for a specific site within a query.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     * @throws SiteNotFoundException  If the given site ID does not identify a known site within a query.
     */
    int getResultFeasibility(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException;

    /**
     * Gets all site IDs associated with a published query that can already provide a result.
     *
     * @param queryId Identifies the query whose associated site IDs with results shall be gotten.
     * @return All site IDs of a specific query that can already provide results.
     * @throws QueryNotFoundException If the given query ID does not identify a known query.
     */
    List<String> getResultSiteIds(String queryId) throws QueryNotFoundException;

    /**
     * Gets the display name of a site.
     *
     * @param siteId Identifies the site whose display name shall be gotten.
     * @return The display name of the site.
     * @throws SiteNotFoundException If the given site ID does not identify a known site.
     */
    String getSiteName(String siteId) throws SiteNotFoundException;
}
