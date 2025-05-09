package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.Query;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.query.api.status.QueryQuota;
import de.numcodex.feasibility_gui_backend.query.api.status.QueryQuotaEntry;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.RandomSiteNameGenerator;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryHandlerService {

    public enum ResultDetail {
        SUMMARY,
        DETAILED_OBFUSCATED,
        DETAILED
    }

    @NonNull
    private final QueryDispatcher queryDispatcher;

    @NonNull
    private final QueryRepository queryRepository;

    @NonNull
    private final QueryContentRepository queryContentRepository;

    @NonNull
    private final ResultService resultService;

    @NonNull
    private final StructuredQueryValidation structuredQueryValidation;

    @NonNull
    private ObjectMapper jsonUtil;

    public Mono<Long> runQuery(StructuredQuery structuredQuery, String userId) {
        try {
            var queryId = queryDispatcher.enqueueNewQuery(structuredQuery, userId);
            return queryDispatcher.dispatchEnqueuedQuery(queryId)
                    .thenReturn(queryId);
        } catch (QueryDispatchException e) {
            return Mono.error(e);
        }
    }

    @Transactional
    public QueryResult getQueryResult(Long queryId, ResultDetail resultDetail) {
        var singleSiteResults = resultService.findSuccessfulByQuery(queryId);
        List<QueryResultLine> resultLines = new ArrayList<>();

        if (resultDetail != ResultDetail.SUMMARY) {
            resultLines = singleSiteResults.stream()
                .map(ssr -> QueryResultLine.builder()
                    .siteName(resultDetail == ResultDetail.DETAILED_OBFUSCATED ? RandomSiteNameGenerator.generateRandomSiteName() : ssr.siteName())
                    .numberOfPatients(ssr.result())
                    .build())
                .toList();
        }

        var totalMatchesInPopulation = singleSiteResults.stream()
            .mapToLong(ResultLine::result).sum();

        return QueryResult.builder()
            .queryId(queryId)
            .resultLines(resultLines)
            .totalNumberOfPatients(totalMatchesInPopulation)
            .build();
    }

    public Query getQuery(Long queryId) throws JsonProcessingException {
        var query = queryRepository.findById(queryId);
        if (query.isPresent()) {
            return convertQueryToApi(query.get());
        } else {
            return null;
        }
    }

    public StructuredQuery getQueryContent(Long queryId) throws JsonProcessingException {
        var queryContent = queryContentRepository.findByQueryId(queryId);
        if (queryContent.isPresent()) {
            return jsonUtil.readValue(queryContent.get().getQueryContent(), StructuredQuery.class);
        } else {
            return null;
        }
    }


    private Query convertQueryToApi(de.numcodex.feasibility_gui_backend.query.persistence.Query in)
        throws JsonProcessingException {

        return Query.builder()
                .id(in.getId())
                .content(jsonUtil.readValue(in.getQueryContent().getQueryContent(), StructuredQuery.class))
                .build();
    }

    public String getAuthorId(Long queryId) throws QueryNotFoundException {
        return queryRepository.getAuthor(queryId).orElseThrow(QueryNotFoundException::new);
    }

    public Long getAmountOfQueriesByUserAndInterval(String userId, String interval) {
        return queryRepository.countQueriesByAuthorInTheLastNMinutes(userId, Duration.parse(interval).toMinutes());
    }

    public Long getRetryAfterTime(String userId, int offset, String interval) {
        try {
            return Duration.parse(interval).toSeconds() - queryRepository.getAgeOfNToLastQueryInSeconds(userId, offset) + 1;
        } catch (NullPointerException e) {
            return 0L;
        }
    }

  public QueryQuota getSentQueryStatistics(String userName, int softAmount, String softInterval, int hardAmount, String hardInterval) {
    var softUsed = queryRepository.countQueriesByAuthorInTheLastNMinutes(userName, Duration.parse(softInterval).toMinutes());
    var hardUsed = queryRepository.countQueriesByAuthorInTheLastNMinutes(userName, Duration.parse(hardInterval).toMinutes());

    return QueryQuota.builder()
        .soft(QueryQuotaEntry.builder()
            .interval(softInterval)
            .limit(softAmount)
            .used(softUsed.intValue())
            .build())
        .hard(QueryQuotaEntry.builder()
            .interval(hardInterval)
            .limit(hardAmount)
            .used(hardUsed.intValue())
            .build())
        .build();
  }
}
