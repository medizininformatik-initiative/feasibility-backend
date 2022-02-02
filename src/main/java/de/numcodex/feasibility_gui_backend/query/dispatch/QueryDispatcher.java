package de.numcodex.feasibility_gui_backend.query.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.UnsupportedMediaTypeException;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryDispatch.QueryDispatchId;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationComponent;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Centralized component to enqueue and dispatch (publish) a {@link StructuredQuery}.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
public class QueryDispatcher {

    @NonNull
    private List<BrokerClient> queryBrokerClients;

    @NonNull
    private QueryTranslationComponent queryTranslationComponent;

    @NonNull
    private QueryHashCalculator queryHashCalculator;

    @NonNull
    private ObjectMapper jsonUtil;

    @NonNull
    private QueryRepository queryRepository;

    @NonNull
    private QueryContentRepository queryContentRepository;

    @NonNull
    private QueryDispatchRepository queryDispatchRepository;

    /**
     * Enqueues a {@link StructuredQuery}, allowing it to be published afterwards. Enqueued queries are stored within
     * the database as a side effect.
     *
     * @param query The query that shall be enqueued.
     * @return Identifier of the enqueued query. This acts as a reference when trying to publish it.
     * @throws QueryDispatchException If an error occurs while enqueueing the query.
     */
    // TODO: Pass in audit information! (actor)
    public Long enqueueNewQuery(StructuredQuery query) throws QueryDispatchException {
        var querySerialized = serializedStructuredQuery(query);

        var queryHash = queryHashCalculator.calculateSerializedQueryBodyHash(querySerialized);
        var queryBody = queryContentRepository.findByHash(queryHash)
                .orElseGet(() -> {
                    var freshQueryBody = new QueryContent(querySerialized);
                    freshQueryBody.setHash(queryHash);
                    return queryContentRepository.save(freshQueryBody);
                });

        var queryId = persistEnqueuedQuery(queryBody);
        log.info("enqueued query '%s'".formatted(queryId));
        return queryId;
    }

    /**
     * Dispatches (publishes) an already enqueued query in a broadcast fashion using configured {@link BrokerClient}s.
     *
     * @param queryId Identifies the backend query that shall be dispatched.
     * @throws QueryDispatchException If an error occurs while dispatching the query.
     */
    // TODO: Pass in audit information! (actor)
    public void dispatchEnqueuedQuery(Long queryId) throws QueryDispatchException {
        var enqueuedQuery = getEnqueuedQuery(queryId);
        var deserializedQueryBody = getStructuredQueryFromEnqueuedQuery(enqueuedQuery);
        var translatedQueryBodyFormats = translateQueryIntoTargetFormats(deserializedQueryBody);

        // TODO: error handling + asynchronous dispatch!
        try {
            for (BrokerClient broker : queryBrokerClients) {
                var brokerQueryId = broker.createQuery(queryId);

                for (Entry<QueryMediaType, String> queryBodyFormats : translatedQueryBodyFormats.entrySet()) {
                    broker.addQueryDefinition(brokerQueryId, queryBodyFormats.getKey().getRepresentation(),
                            queryBodyFormats.getValue());
                }
                broker.publishQuery(brokerQueryId);
                persistDispatchedQuery(enqueuedQuery, brokerQueryId, broker.getBrokerType());
                log.info("dispatched query '%s' as '%s' with broker type '%s'".formatted(queryId, brokerQueryId,
                        broker.getBrokerType()));
            }
        } catch (UnsupportedMediaTypeException | QueryNotFoundException | IOException e) {
            throw new QueryDispatchException("cannot publish query with id '%s'".formatted(queryId), e);
        }
    }

    private String serializedStructuredQuery(StructuredQuery query) throws QueryDispatchException {
        try {
            return jsonUtil.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new QueryDispatchException("could not serialize query in order to enqueue it", e);
        }
    }

    private Long persistEnqueuedQuery(QueryContent queryBody) {
        var feasibilityQuery = new Query();
        feasibilityQuery.setCreatedAt(Timestamp.from(Instant.now()));
        feasibilityQuery.setQueryContent(queryBody);
        return queryRepository.save(feasibilityQuery).getId();
    }

    private void persistDispatchedQuery(Query query, String brokerInternalId, BrokerClientType brokerType) {
        var queryDispatchId = new QueryDispatchId();
        queryDispatchId.setQueryId(query.getId());
        queryDispatchId.setExternalId(brokerInternalId);
        queryDispatchId.setBrokerType(brokerType);

        var dispatchedQuery = new QueryDispatch();
        dispatchedQuery.setId(queryDispatchId);
        dispatchedQuery.setQuery(query);
        dispatchedQuery.setDispatchedAt(Timestamp.from(Instant.now()));
        queryDispatchRepository.save(dispatchedQuery);
    }

    private Query getEnqueuedQuery(Long queryId) throws QueryDispatchException {
        return queryRepository.findById(queryId)
                .orElseThrow(() ->
                        new QueryDispatchException("cannot find enqueued query with id '%s'".formatted(queryId)));
    }

    private StructuredQuery getStructuredQueryFromEnqueuedQuery(Query enqueuedQuery) throws QueryDispatchException {
        try {
            return jsonUtil.readValue(enqueuedQuery.getQueryContent().getQueryContent(), StructuredQuery.class);
        } catch (JsonProcessingException e) {
            throw new QueryDispatchException("cannot deserialize enqueued query body as structured query", e);
        }
    }

    private Map<QueryMediaType, String> translateQueryIntoTargetFormats(StructuredQuery query)
            throws QueryDispatchException {
        try {
            return queryTranslationComponent.translate(query);
        } catch (QueryTranslationException e) {
            throw new QueryDispatchException("cannot translate enqueued query body into configured formats", e);
        }
    }
}
