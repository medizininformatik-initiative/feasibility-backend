package QueryExecutor.impl.dsf;

import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.ClientNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * Manages {@link DSFQueryResult}s.
 */
class DSFQueryResultStore implements QueryResultStore {

    private final Map<String, Map<String, Integer>> results;

    public DSFQueryResultStore() {
        this.results = new HashMap<>();
    }


    @Override
    public void storeResult(@NotNull DSFQueryResult result) {
        Map<String, Integer> resultsPerClient = results.get(result.getQueryId());
        if (resultsPerClient == null) {
            resultsPerClient = new HashMap<>();
            resultsPerClient.put(result.getClientId(), result.getMeasureCount());
            results.put(result.getQueryId(), resultsPerClient);
        } else {
            resultsPerClient.put(result.getClientId(), result.getMeasureCount());
        }
    }

    @Override
    public int getMeasureCount(String queryId, String clientId) throws QueryNotFoundException, ClientNotFoundException {
        Map<String, Integer> resultsPerClient = results.get(queryId);
        if (resultsPerClient == null) {
            throw new QueryNotFoundException(queryId);
        }

        Integer count = resultsPerClient.get(clientId);
        if (count == null) {
            throw new ClientNotFoundException(queryId, clientId);
        }

        return count;
    }

    @Override
    public List<String> getClientIdsWithResult(String queryId) throws QueryNotFoundException {
        Map<String, Integer> resultsPerClient = results.get(queryId);
        if (resultsPerClient == null) {
            throw new QueryNotFoundException(queryId);
        }

        return List.copyOf(resultsPerClient.keySet());
    }

    @Override
    public void removeResult(String queryId) throws QueryNotFoundException {
        Map<String, Integer> resultsPerClient = results.get(queryId);
        if (resultsPerClient == null) {
            throw new QueryNotFoundException(queryId);
        }

        results.remove(queryId);
    }
}
