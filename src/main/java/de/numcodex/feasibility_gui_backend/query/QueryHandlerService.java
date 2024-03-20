package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.Query;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.SavedQuery;
import de.numcodex.feasibility_gui_backend.query.api.*;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.RandomSiteNameGenerator;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateException;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateHandler;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final QueryTemplateHandler queryTemplateHandler;

    @NonNull
    private final QueryRepository queryRepository;

    @NonNull
    private final QueryContentRepository queryContentRepository;

    @NonNull
    private final ResultService resultService;

    @NonNull
    private final QueryTemplateRepository queryTemplateRepository;

    @NonNull
    private final SavedQueryRepository savedQueryRepository;

    @NonNull
    private final TermCodeValidation termCodeValidation;

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
        var savedQuery = savedQueryRepository.findByQueryId(queryId);
        if (query.isPresent()) {
            return convertQueryToApi(query.get(), savedQuery);
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

    public Long storeQueryTemplate(QueryTemplate queryTemplate, String userId)
            throws QueryTemplateException {
        return queryTemplateHandler.storeTemplate(queryTemplate, userId);
    }

    public Long saveQuery(Long queryId, String userId, SavedQuery savedQueryApi) {
        if (savedQueryRepository.existsSavedQueryByLabelAndUserId(savedQueryApi.label(), userId)) {
            throw new DataIntegrityViolationException(String.format("User %s already has a saved query named %s", userId, savedQueryApi.label()));
        }
        de.numcodex.feasibility_gui_backend.query.persistence.SavedQuery savedQuery = convertSavedQueryApiToPersistence(savedQueryApi, queryId);
        return savedQueryRepository.save(savedQuery).getId();
    }

    public void updateSavedQuery(Long queryId, SavedQuery savedQuery) throws QueryNotFoundException {
        Optional<de.numcodex.feasibility_gui_backend.query.persistence.SavedQuery> savedQueryOptional = savedQueryRepository.findByQueryId(queryId);
        if (savedQueryOptional.isEmpty()) {
            throw new QueryNotFoundException();
        }
        var oldSavedQuery = savedQueryOptional.get();
        oldSavedQuery.setLabel(savedQuery.label());
        oldSavedQuery.setComment(savedQuery.comment());
        savedQueryRepository.save(oldSavedQuery);
    }

    public de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate getQueryTemplate(
            Long queryId, String authorId) throws QueryTemplateException {
        de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate queryTemplate = queryTemplateRepository.findById(
                queryId).orElseThrow(QueryTemplateException::new);
        if (!queryTemplate.getQuery().getCreatedBy().equalsIgnoreCase(authorId)) {
            throw new QueryTemplateException();
        }
        return queryTemplate;
    }

    public List<de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate> getQueryTemplatesForAuthor(
            String authorId) {
        return queryTemplateRepository.findByAuthor(authorId);
    }

    public void updateQueryTemplate(Long queryTemplateId, QueryTemplate queryTemplate, String authorId) throws QueryTemplateException {
        var templates = getQueryTemplatesForAuthor(authorId);
        Optional<de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate> templateToUpdate = templates.stream().
                filter(t -> t.getId().equals(queryTemplateId)).
                findFirst();

        if (templateToUpdate.isPresent()) {
            var template = templateToUpdate.get();
            template.setLabel(queryTemplate.label());
            template.setComment(queryTemplate.comment());
            template.setLastModified(Timestamp.from(Instant.now()));
            queryTemplateRepository.save(template);
        } else {
            throw new QueryTemplateException("not found");
        }
    }

    public void deleteQueryTemplate(Long queryTemplateId, String authorId) throws QueryTemplateException {
        var templates = getQueryTemplatesForAuthor(authorId);
        Optional<de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate> templateToDelete = templates.stream().
                filter(t -> t.getId().equals(queryTemplateId)).
                findFirst();

        if (templateToDelete.isPresent()) {
            queryTemplateRepository.delete(templateToDelete.get());
        } else {
            throw new QueryTemplateException("not found");
        }
    }

    public QueryTemplate convertTemplatePersistenceToApi(
            de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate in)
            throws JsonProcessingException {
        return queryTemplateHandler.convertPersistenceToApi(in);
    }

    private Query convertQueryToApi(de.numcodex.feasibility_gui_backend.query.persistence.Query in,
                                    Optional<de.numcodex.feasibility_gui_backend.query.persistence.SavedQuery> savedQuery)
            throws JsonProcessingException {

        if (savedQuery.isPresent()) {
            return Query.builder()
                    .id(in.getId())
                    .content(jsonUtil.readValue(in.getQueryContent().getQueryContent(), StructuredQuery.class))
                    .label(savedQuery.get().getLabel())
                    .comment(savedQuery.get().getComment())
                    .build();
        } else {
            return Query.builder()
                    .id(in.getId())
                    .content(jsonUtil.readValue(in.getQueryContent().getQueryContent(), StructuredQuery.class))
                    .build();
        }
    }

    private de.numcodex.feasibility_gui_backend.query.persistence.SavedQuery convertSavedQueryApiToPersistence(
            SavedQuery in, Long queryId) {
        var out = new de.numcodex.feasibility_gui_backend.query.persistence.SavedQuery();
        out.setQuery(queryRepository.getReferenceById(queryId));
        out.setComment(in.comment());
        out.setLabel(in.label());
        out.setResultSize(in.totalNumberOfPatients());
        return out;
    }

    public List<de.numcodex.feasibility_gui_backend.query.persistence.Query> getQueryListForAuthor(
            String userId, boolean savedOnly) {
        Optional<List<de.numcodex.feasibility_gui_backend.query.persistence.Query>> queries;

        if (savedOnly) {
            queries = queryRepository.findSavedQueriesByAuthor(userId);
        } else {
            queries = queryRepository.findByAuthor(userId);
        }

        return queries.orElseGet(ArrayList::new);
    }

    public String getAuthorId(Long queryId) throws QueryNotFoundException {
        return queryRepository.getAuthor(queryId).orElseThrow(QueryNotFoundException::new);
    }

    public QueryListEntry convertQueryToQueryListEntry(de.numcodex.feasibility_gui_backend.query.persistence.Query query,
                                                       boolean skipValidation) {
        boolean isValid = true;
        if (!skipValidation) {
            try {
                var sq = jsonUtil.readValue(query.getQueryContent().getQueryContent(), StructuredQuery.class);
                isValid = termCodeValidation.isValid(sq);
            } catch (JsonProcessingException e) {
                isValid = false;
            }
        }

        if (query.getSavedQuery() != null) {
            if (skipValidation) {
                return
                    QueryListEntry.builder()
                        .id(query.getId())
                        .label(query.getSavedQuery().getLabel())
                        .comment(query.getSavedQuery().getComment())
                        .totalNumberOfPatients(query.getSavedQuery().getResultSize())
                        .createdAt(query.getCreatedAt())
                        .build();
            } else {
                return
                    QueryListEntry.builder()
                        .id(query.getId())
                        .label(query.getSavedQuery().getLabel())
                        .comment(query.getSavedQuery().getComment())
                        .totalNumberOfPatients(query.getSavedQuery().getResultSize())
                        .createdAt(query.getCreatedAt())
                        .isValid(isValid)
                        .build();
            }
        } else {
            if (skipValidation) {
                return
                    QueryListEntry.builder()
                        .id(query.getId())
                        .createdAt(query.getCreatedAt())
                        .build();
            } else {
                return
                    QueryListEntry.builder()
                        .id(query.getId())
                        .createdAt(query.getCreatedAt())
                        .isValid(isValid)
                        .build();
            }
        }
    }

    public List<QueryListEntry> convertQueriesToQueryListEntries(List<de.numcodex.feasibility_gui_backend.query.persistence.Query> queryList,
                                                                 boolean skipValidation) {
        var ret = new ArrayList<QueryListEntry>();
        queryList.forEach(q -> ret.add(convertQueryToQueryListEntry(q, skipValidation)));
        return ret;
    }

    public Long getAmountOfQueriesByUserAndInterval(String userId, int minutes) {
        return queryRepository.countQueriesByAuthorInTheLastNMinutes(userId, minutes);
    }

    public Long getRetryAfterTime(String userId, int offset, long interval) {
        try {
            return 60 * interval - queryRepository.getAgeOfNToLastQueryInSeconds(userId, offset) + 1;
        } catch (NullPointerException e) {
            return 0L;
        }
    }

    public Long getAmountOfSavedQueriesByUser(String userId) {
        var queries = queryRepository.findSavedQueriesByAuthor(userId);
        return queries.map(queryList -> (long) queryList.size()).orElse(0L);
    }

    public void deleteSavedQuery(Long queryId) throws QueryNotFoundException {
        var savedQueryOptional = savedQueryRepository.findByQueryId(queryId);
        if (savedQueryOptional.isPresent()) {
            savedQueryRepository.deleteById(savedQueryOptional.get().getId());
        } else {
            throw new QueryNotFoundException();
        }
    }
}
