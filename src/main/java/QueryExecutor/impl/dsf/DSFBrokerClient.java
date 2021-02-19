package QueryExecutor.impl.dsf;

import QueryExecutor.api.BrokerClient;
import QueryExecutor.api.ClientNotFoundException;
import QueryExecutor.api.Listener;
import QueryExecutor.api.PublishFailedException;
import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.UnsupportedMediaTypeException;

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
    public void addListener(Listener listener) {
        queryResultCollector.addResultListener(listener);
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
    public void publishQuery(String queryId) throws QueryNotFoundException, PublishFailedException {
        queryManager.publishQuery(queryId);
    }

    @Override
    public void closeQuery(String queryId) throws QueryNotFoundException {
        queryManager.removeQuery(queryId);
        queryResultCollector.removeResults(queryId);
    }

    @Override
    public int getResultFeasibility(String queryId, String clientId) throws QueryNotFoundException, ClientNotFoundException {
        return queryResultCollector.getResultFeasibility(queryId, clientId);
    }

    @Override
    public List<String> getResultClientIds(String queryId) throws QueryNotFoundException {
        return queryResultCollector.getResultClientIds(queryId);
    }
}
