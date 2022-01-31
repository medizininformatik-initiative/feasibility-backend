package de.numcodex.feasibility_gui_backend.query.broker.direct;

import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusUpdate;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.*;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.STRUCTURED_QUERY;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.FAILED;
import static de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType.DIRECT;

/**
 * A {@link BrokerClient} to be used to directly communicate with a Flare instance without the need for using on of the
 * other middlewares (Aktin or DSF).
 */
@Slf4j
public class DirectBrokerClient implements BrokerClient {

    private static final String SITE_1_ID = "1";
    private static final String SITE_1_NAME = "FHIR Server";

    private static final String FLARE_REQUEST_CONTENT_TYPE = "application/json";
    private static final String FLARE_RESPONSE_ACCEPT_CONTENT_TYPE = "CSQ";
    private static final String FLARE_QUERY_ENDPOINT_URL = "/query/execute";

    private final WebClient webClient;
    private final List<QueryStatusListener> listeners;
    private final Map<String, DirectQuery> brokerQueries;
    private final Map<String, Long> brokerToBackendQueryIdMapping;

    /**
     * Creates a new {@link DirectBrokerClient} instance that uses the given web client to communicate with a Flare
     * instance.
     *
     * @param webClient A web client to communicate with a Flare instance.
     */
    public DirectBrokerClient(WebClient webClient) {
        this.webClient = Objects.requireNonNull(webClient);
        listeners = new ArrayList<>();
        brokerQueries = new HashMap<>();
        brokerToBackendQueryIdMapping = new HashMap<>();
    }

    @Override
    public BrokerClientType getBrokerType() {
        return DIRECT;
    }

    @Override
    public void addQueryStatusListener(QueryStatusListener queryStatusListener) {
        listeners.add(queryStatusListener);
    }

    @Override
    public String createQuery(Long backendQueryId) {
        var brokerQuery = DirectQuery.create();
        var brokerQueryId = brokerQuery.getQueryId();
        brokerQueries.put(brokerQueryId, brokerQuery);
        brokerToBackendQueryIdMapping.put(brokerQueryId, backendQueryId);

        return brokerQueryId;
    }

    @Override
    public void addQueryDefinition(String brokerQueryId, String mediaType, String content) throws QueryNotFoundException {
        findQuery(brokerQueryId).addQueryDefinition(mediaType, content);
    }

    @Override
    public void publishQuery(String brokerQueryId) throws QueryNotFoundException, IOException {
        var query = findQuery(brokerQueryId);
        var structuredQueryContent = Optional.ofNullable(query.getQueryDefinition(STRUCTURED_QUERY))
                .orElseThrow(() -> new IllegalStateException("Query with ID "
                        + brokerQueryId
                        + " does not contain a query definition for the mandatory type: "
                        + STRUCTURED_QUERY
                ));

        try {
            webClient.post()
                    .uri(FLARE_QUERY_ENDPOINT_URL)
                    .header(HttpHeaders.CONTENT_TYPE, FLARE_REQUEST_CONTENT_TYPE)
                    // TODO: Resolve this with the Flare team. This is NOT the header to be used.
                    //       The accept encoding header should not change the content itself.
                    //       Thus, it's mainly used for compression algorithms.
                    .header(HttpHeaders.ACCEPT_ENCODING, FLARE_RESPONSE_ACCEPT_CONTENT_TYPE)
                    .bodyValue(structuredQueryContent)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(Integer::valueOf)
                    .doOnError(error -> {
                        log.error(error.getMessage(), error);
                        var statusUpdate = new QueryStatusUpdate(this, brokerQueryId, SITE_1_ID, FAILED);
                        var associatedBackendQueryId = brokerToBackendQueryIdMapping.get(brokerQueryId);
                        listeners.forEach(l -> l.onClientUpdate(associatedBackendQueryId, statusUpdate));
                    })
                    .subscribe(val -> {
                        query.registerSiteResults(SITE_1_ID, val);
                        var statusUpdate = new QueryStatusUpdate(this, brokerQueryId, SITE_1_ID, COMPLETED);
                        var associatedBackendQueryId = brokerToBackendQueryIdMapping.get(brokerQueryId);
                        listeners.forEach(l -> l.onClientUpdate(associatedBackendQueryId, statusUpdate));
                    });
        } catch (Exception e) {
            throw new IOException("An error occurred while publishing the query with ID: " + brokerQueryId, e);
        }
    }

    @Override
    public void closeQuery(String brokerQueryId) throws QueryNotFoundException {
        if (brokerQueries.remove(brokerQueryId) == null) {
            throw new QueryNotFoundException(brokerQueryId);
        }
    }

    @Override
    public int getResultFeasibility(String brokerQueryId, String siteId) throws QueryNotFoundException, SiteNotFoundException {
        return findQuery(brokerQueryId).getSiteResult(siteId);
    }

    @Override
    public List<String> getResultSiteIds(String brokerQueryId) throws QueryNotFoundException {
        return findQuery(brokerQueryId).getSiteIdsWithResult();
    }

    @Override
    public String getSiteName(String siteId) {
        return siteId.equals(SITE_1_ID) ? SITE_1_NAME : "";
    }

    /**
     * Searches for a {@link DirectQuery} within this broker. The specified query ID identifies the query.
     *
     * @param queryId The identifier of the query.
     * @return The query if there is any.
     * @throws QueryNotFoundException If the ID does not identify a known query.
     */
    private DirectQuery findQuery(String queryId) throws QueryNotFoundException {
        return Optional.ofNullable(brokerQueries.get(queryId))
                .orElseThrow(() -> new QueryNotFoundException(queryId));
    }

    /**
     * A data container representing a query used for direct communications with a Flare instance.
     */
    static class DirectQuery {
        @Getter
        private final String queryId;
        private final Map<String, String> queryDefinitions;
        private final Map<String, Integer> resultsBySite;

        private DirectQuery(String queryId) {
            this.queryId = queryId;
            queryDefinitions = new HashMap<>();
            resultsBySite = new HashMap<>();
        }

        /**
         * Creates a new {@link DirectQuery} with a random UUID as a query ID.
         *
         * @return The created query.
         */
        public static DirectQuery create() {
            return new DirectQuery(UUID.randomUUID().toString());
        }

        /**
         * Adds a query definition. A query definition is a query in a specific format (i.e. structured query / CQL).
         * The specified mime type defines the format of the query itself.
         * When invoked multiple times for a single mime type, any already existing query content associated with this
         * mime type gets overwritten.
         * <p>
         * For more information on available mime types see {@link QueryMediaType}.
         *
         * @param mimeType The mime type defining the format of the query.
         * @param content  The actual query in its string representation.
         */
        public void addQueryDefinition(String mimeType, String content) {
            queryDefinitions.put(mimeType, content);
        }

        /**
         * Gets a single query definition associated with the given mime type.
         *
         * @param mimeType The mime type (query format) of the query.
         * @return The query in its string representation or null if there is no query definition associated with the
         * specified mime type.
         */
        public String getQueryDefinition(QueryMediaType mimeType) {
            return queryDefinitions.get(mimeType.getRepresentation());
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
