package de.numcodex.feasibility_gui_backend.service.query_executor;

/**
 * Represents an entity capable of receiving results from a feasibility query running in a distributed fashion.
 */
public interface QueryStatusListener {

    /**
     * Callback method to process feasibility query result updates.
     *
     * @param queryId Identifies the query for which there is an update.
     * @param siteId  Identifies the site within the query for which there is an update.
     * @param status  State of the query.
     */
    void onClientUpdate(String queryId, String siteId, QueryStatus status);
}
