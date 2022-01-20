package de.numcodex.feasibility_gui_backend.query.broker.mock;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.FAILED;
import static de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType.MOCK;

/**
 * A {@link BrokerClient} to be used when results need to be mocked. This broker mocks a middleware an all the other
 * downstream parts by simply generating random results no matter what query it runs.
 * <p>
 * This should never be used in production!
 */
@Slf4j
public class MockBrokerClient implements BrokerClient {

    private final List<QueryStatusListener> listeners;
    private final Map<String, MockQuery> queries;
    private final Map<String, String> siteNames;

    private final Map<String, List<CompletableFuture<Void>>> runningMocks;

    public MockBrokerClient() {
        listeners = new ArrayList<>();
        queries = new HashMap<>();
        siteNames = Map.of(
                "2", "Lübeck",
                "3", "Erlangen",
                "4", "Frankfurt",
                "5", "Leipzig"
        );
        runningMocks = new HashMap<>();
    }

    @Override
    public BrokerClientType getBrokerType() {
        return MOCK;
    }

    @Override
    public void addQueryStatusListener(QueryStatusListener queryStatusListener) {
        this.listeners.add(queryStatusListener);
    }

    @Override
    public String createQuery() {
        var query = MockQuery.create();
        queries.put(query.getQueryId(), query);
        runningMocks.put(query.getQueryId(), List.of());

        return query.getQueryId();
    }

    @Override
    public void addQueryDefinition(String queryId, String mediaType, String content) {
        // No-Op since this is irrelevant.
    }

    @Override
    public void publishQuery(String queryId) throws QueryNotFoundException {
        var query = findQuery(queryId);

        var mocks = siteNames.keySet()
                .stream().map(siteId -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(Math.round(2000 + 6000 * Math.random()));
                        query.registerSiteResults(siteId, (int) Math.round(10 + 500 * Math.random()));
                        listeners.forEach(l -> l.onClientUpdate(this, queryId, siteId, COMPLETED));
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                        listeners.forEach(l -> l.onClientUpdate(this, queryId, siteId, FAILED));
                    }
                }))
                .collect(Collectors.toList());
        runningMocks.put(queryId, mocks);
    }

    @Override
    public void closeQuery(String queryId) throws QueryNotFoundException {
        Optional.ofNullable(runningMocks.get(queryId))
                .orElseThrow(() -> new QueryNotFoundException(queryId))
                .forEach(rm -> rm.complete(null));
    }

    @Override
    public int getResultFeasibility(String queryId, String siteId) throws QueryNotFoundException, SiteNotFoundException {
        return findQuery(queryId).getSiteResult(siteId);
    }

    @Override
    public List<String> getResultSiteIds(String queryId) throws QueryNotFoundException {
        return findQuery(queryId).getSiteIdsWithResult();
    }

    @Override
    public String getSiteName(String siteId) {
        return siteNames.getOrDefault(siteId, "");
    }

    /**
     * Searches for a {@link MockQuery} within this broker. The specified query ID identifies the query.
     *
     * @param queryId The identifier of the query.
     * @return The query if there is any.
     * @throws QueryNotFoundException If the ID does not identify a known query.
     */
    private MockQuery findQuery(String queryId) throws QueryNotFoundException {
        return Optional.ofNullable(queries.get(queryId))
                .orElseThrow(() -> new QueryNotFoundException(queryId));
    }

    /**
     * A data container representing a query used for direct communications with a Flare instance.
     */
    static class MockQuery {
        @Getter
        private final String queryId;
        private final Map<String, Integer> resultsBySite;

        private MockQuery(String queryId) {
            this.queryId = queryId;
            resultsBySite = new HashMap<>();
        }

        /**
         * Creates a new {@link MockQuery} with a random UUID as a query ID.
         *
         * @return The created query.
         */
        public static MockQuery create() {
            return new MockQuery(UUID.randomUUID().toString());
        }

        /**
         * Adds the feasibility result of a single site.
         *
         * @param siteId      Identifies the site the feasibility result is linked to.
         * @param feasibility The feasibility result.
         */
        public void registerSiteResults(String siteId, int feasibility) {
            resultsBySite.put(siteId, feasibility);
        }

        /**
         * Gets the feasibility result of a single site.
         *
         * @param siteId Identifies the site in question.
         * @return The feasibility result of the site identified by the specified identifier.
         * @throws SiteNotFoundException If the ID does not identify a known site.
         */
        public int getSiteResult(String siteId) throws SiteNotFoundException {
            return Optional.ofNullable(resultsBySite.get(siteId))
                    .orElseThrow(() -> new SiteNotFoundException(queryId, siteId));
        }

        /**
         * Gets all site identifiers which already reported a feasibility result.
         *
         * @return A list of identifiers of sites.
         */
        public List<String> getSiteIdsWithResult() {
            return new ArrayList<>(resultsBySite.keySet());
        }
    }
}
