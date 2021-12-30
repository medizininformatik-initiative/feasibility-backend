package de.numcodex.feasibility_gui_backend.query.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.db.Query;
import de.numcodex.feasibility_gui_backend.model.db.QueryContent;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationComponent;
import de.numcodex.feasibility_gui_backend.repository.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Centralized component to enqueue and publish a {@link StructuredQuery}.
 */
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
        String querySerialized;
        try {
            querySerialized = jsonUtil.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new QueryDispatchException("could not serialize query in order to enqueue it", e);
        }

        var queryHash = queryHashCalculator.calculateSerializedQueryBodyHash(querySerialized);
        var queryBody = queryContentRepository.findByHash(queryHash)
                .orElseGet(() -> {
                    var freshQueryBody = new QueryContent(querySerialized);
                    freshQueryBody.setHash(queryHash);
                    return queryContentRepository.save(freshQueryBody);
                });

        var feasibilityQuery = new Query();
        feasibilityQuery.setCreatedAt(Timestamp.from(Instant.now()));
        feasibilityQuery.setQueryContent(queryBody);
        return queryRepository.save(feasibilityQuery).getId();
    }


    public void publishEnqueuedQuery(Long queryId) throws QueryDispatchException {
        // TODO: translation needs to happen here in order to allow for replay of saved queries!
    }
}
