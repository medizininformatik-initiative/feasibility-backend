package de.numcodex.feasibility_gui_backend.query.collect;

import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;

/**
 * Represents an entity capable of receiving results from a feasibility query running in a distributed fashion.
 */
public interface QueryStatusListener {

    /**
     * Callback method to process feasibility query result updates.
     *
     * @param client  The broker client that this update belongs to.
     * @param queryId Identifies the query within the broker for which there is an update.
     * @param siteId  Identifies the site within the broker for which there is an update. Related to a specific query.
     * @param status  New state of the query.
     */
    void onClientUpdate(BrokerClient client, String queryId, String siteId, QueryStatus status);
}
