package QueryExecutor.impl.dsf;

import QueryExecutor.api.ClientNotFoundException;
import QueryExecutor.api.Listener;
import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.QueryStatus;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.highmed.fhir.client.WebsocketClient;
import org.hl7.fhir.r4.model.DomainResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Collector for collecting the results of feasibility queries that are running in a distributed fashion.
 * <p>
 * The collector gathers query results from a single FHIR server. Communication with this FHIR server
 * happens using a websocket. The FHIR server sends all task resources that are associated with a subscription.
 */
class DSFQueryResultCollector implements QueryResultCollector {

    private final QueryResultStore store;
    private final FhirContext fhirContext;
    private final WebsocketClient fhirWebsocketClient;
    private final DSFQueryResultHandler resultHandler;
    private final List<Listener> listeners;
    private boolean websocketConnectionEstablished;

    /**
     * Creates a new {@link DSFQueryResultCollector}.
     *
     * @param store               Storage facility for storing collected results.
     * @param fhirContext         The FHIR context used for communication purposes with the FHIR server results are
     *                            gathered from.
     * @param fhirWebsocketClient Client able to connect to a FHIR server using a websocket for receiving query results.
     * @param resultHandler       Handler able to process query results received from the FHIR server.
     */
    public DSFQueryResultCollector(QueryResultStore store, FhirContext fhirContext, WebsocketClient fhirWebsocketClient, DSFQueryResultHandler resultHandler) {
        this.store = store;
        this.fhirContext = fhirContext;
        this.fhirWebsocketClient = fhirWebsocketClient;
        this.resultHandler = resultHandler;
        this.websocketConnectionEstablished = false;
        this.listeners = new ArrayList<>();
    }

    private void listenForQueryResults() {
        if (!websocketConnectionEstablished) {
            fhirWebsocketClient.setDomainResourceHandler(this::setUpQueryResultHandler, this::setUpResourceParser);

            fhirWebsocketClient.connect();
            websocketConnectionEstablished = true;
        }
    }

    private void setUpQueryResultHandler(DomainResource resource) {
        resultHandler.onResult(resource).ifPresent((res) -> {
            store.storeResult(res);
            notifyResultListeners(res);
        });
    }

    private IParser setUpResourceParser() {
        return fhirContext.newJsonParser()
                .setStripVersionsFromReferences(false)
                .setOverrideResourceIdWithBundleEntryFullUrl(false);
    }

    private void notifyResultListeners(DSFQueryResult result) {
        for (Listener listener : listeners) {
            listener.onClientUpdate(result.getQueryId(), result.getClientId(), QueryStatus.COMPLETED);
        }
    }

    @Override
    public void addResultListener(Listener listener) {
        listeners.add(listener);
        listenForQueryResults();
    }

    @Override
    public int getResultFeasibility(String queryId, String clientId) throws QueryNotFoundException, ClientNotFoundException {
        return store.getMeasureCount(queryId, clientId);
    }

    @Override
    public List<String> getResultClientIds(String queryId) throws QueryNotFoundException {
        return store.getClientIdsWithResult(queryId);
    }

    @Override
    public void removeResults(String queryId) throws QueryNotFoundException {
        store.removeResult(queryId);
    }
}
