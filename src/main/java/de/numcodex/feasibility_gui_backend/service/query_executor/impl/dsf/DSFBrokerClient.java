package de.numcodex.feasibility_gui_backend.service.query_executor.impl.dsf;


import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;

import java.io.IOException;
import java.util.List;

/**
 * A {@link BrokerClient} to be used in conjunction with a Data Sharing Framework (DSF) instance.
 */
public final class DSFBrokerClient implements BrokerClient {
    private final QueryManager queryManager;
    private final QueryResultCollector queryResultCollector;

    /**
     * Creates a new {@link DSFBrokerClient} instance.
     *
     * @param queryManager         Manager capable of setting up queries and managing them throughout their lifecycle.
     * @param queryResultCollector Collector for collecting results of running feasibility queries.
     */
    public DSFBrokerClient(QueryManager queryManager, QueryResultCollector queryResultCollector) {
        this.queryManager = queryManager;
        this.queryResultCollector = queryResultCollector;
    }

    @Override
    public void addQueryStatusListener(QueryStatusListener queryStatusListener) {
        queryResultCollector.addResultListener(queryStatusListener);
    }

    @Override
    public String createQuery() {
        return queryManager.createQuery();
    }

    @Override
    public void addQueryDefinition(String queryId, String mediaType, String content) throws QueryNotFoundException,
            UnsupportedMediaTypeException {
        queryManager.addQueryDefinition(queryId, mediaType, content);
    }

    @Override
    public void publishQuery(String queryId) throws QueryNotFoundException, IOException {
        queryManager.publishQuery(queryId);
    }

    @Override
    public void closeQuery(String queryId) throws QueryNotFoundException {
        queryManager.removeQuery(queryId);
        queryResultCollector.removeResults(queryId);
    }

    @Override
    public int getResultFeasibility(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException {
        return queryResultCollector.getResultFeasibility(queryId, siteId);
    }

    @Override
    public List<String> getResultSiteIds(String queryId) throws QueryNotFoundException {
        return queryResultCollector.getResultSiteIds(queryId);
    }

    @Override
    public String getSiteName(String siteId) throws SiteNotFoundException {
        // TODO: implement (separate issue)
        return null;
    }
}
