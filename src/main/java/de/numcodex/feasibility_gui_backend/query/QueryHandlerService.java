package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultLine;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryResultObfuscator;
import de.numcodex.feasibility_gui_backend.query.persistence.Result;
import de.numcodex.feasibility_gui_backend.query.persistence.ResultRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplateRepository;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateException;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateHandler;
import java.util.List;
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
    private final QueryTemplateHandler queryTemplateHandler;

    @NonNull
    private final ResultRepository resultRepository;

    @NonNull
    private final QueryTemplateRepository queryTemplateRepository;

    @NonNull
    private final QueryResultObfuscator queryResultObfuscator;

    public Long runQuery(StructuredQuery structuredQuery, String userId)
        throws QueryDispatchException {
        var queryId = queryDispatcher.enqueueNewQuery(structuredQuery, userId);
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

    public Long storeQueryTemplate(QueryTemplate queryTemplate, String userId)
        throws QueryTemplateException {
        return queryTemplateHandler.storeTemplate(queryTemplate, userId);
    }

    public de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate getQueryTemplate(
        Long queryId, String authorId)  {
        de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate queryTemplate = queryTemplateRepository.findById(
            queryId).orElseThrow();
        if (queryTemplate.getQuery().getCreatedBy().equalsIgnoreCase(authorId)) {
            return queryTemplate;
        } else {
            return null;
        }
    }

    public List<de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate> getQueryTemplatesForAuthor(
        String authorId) {
        return queryTemplateRepository.findByAuthor(authorId);
    }

    public de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate convertTemplateApiToPersistence(
        QueryTemplate in, Long queryId) {
        return queryTemplateHandler.convertApiToPersistence(in, queryId);
    }

    public QueryTemplate convertTemplatePersistenceToApi(
        de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate in)
        throws JsonProcessingException {
        return queryTemplateHandler.convertPersistenceToApi(in);
    }
}
