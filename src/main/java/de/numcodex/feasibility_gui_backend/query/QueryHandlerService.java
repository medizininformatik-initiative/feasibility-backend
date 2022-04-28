package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultLine;
import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.conversion.StoredQueryConverter;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryResultObfuscator;
import de.numcodex.feasibility_gui_backend.query.persistence.Result;
import de.numcodex.feasibility_gui_backend.query.persistence.ResultRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.StoredQueryRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;

@Service
@RequiredArgsConstructor
public class QueryHandlerService {

    @NonNull
    private final QueryDispatcher queryDispatcher;

    @NonNull
    private final ResultRepository resultRepository;

    @NonNull
    private final StoredQueryRepository storedQueryRepository;

    @NonNull
    private final QueryResultObfuscator queryResultObfuscator;

    public Long runQuery(StructuredQuery structuredQuery) throws QueryDispatchException {
        var queryId = queryDispatcher.enqueueNewQuery(structuredQuery);
        queryDispatcher.dispatchEnqueuedQuery(queryId);
        return queryId;
    }

    @Transactional
    public QueryResult getQueryResult(Long queryId) {
        var singleSiteResults = resultRepository.findByQueryAndStatus(queryId, SUCCESS);

        var resultLines = singleSiteResults.stream()
                .map(ssr -> QueryResultLine.builder()
                        .siteName(queryResultObfuscator.tokenizeSiteName(ssr))
                        .numberOfPatients(ssr.getResult())
                        .build())
                .collect(Collectors.toList());

        var totalMatchesInPopulation = singleSiteResults.stream()
                .map(Result::getResult)
                .reduce(0, Integer::sum);

        return QueryResult.builder()
                .queryId(queryId)
                .resultLines(resultLines)
                .totalNumberOfPatients(totalMatchesInPopulation)
                .build();
    }

    public Long storeQuery(StoredQuery query) throws JsonProcessingException {
        ObjectMapper jsonUtil = new ObjectMapper();
        de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery storedQuery = StoredQueryConverter.convertApiToPersistence(query);
        // TODO: get from access token
        storedQuery.setCreatedBy("test");
        de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery test = storedQueryRepository.save(storedQuery);
        return test.getId();
    }

    public de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery getQuery(
        Long queryId) {
        return storedQueryRepository.findById(queryId).orElseThrow();
    }

    public List<de.numcodex.feasibility_gui_backend.query.persistence.StoredQuery> getQueriesForAuthor(
        String authorId) {
        return storedQueryRepository.findByAuthor(authorId);
    }
}
