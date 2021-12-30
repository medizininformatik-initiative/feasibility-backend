package de.numcodex.feasibility_gui_backend.service;

import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.QueryResultLine;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@Transactional
@RequiredArgsConstructor
public class QueryHandlerService {

    @NonNull
    private final QueryDispatcher queryDispatcher;

    @NonNull
    private final ResultRepository resultRepository;

    public Long runQuery(StructuredQuery structuredQuery) throws QueryDispatchException {
        var queryId = queryDispatcher.enqueueNewQuery(structuredQuery);
        queryDispatcher.publishEnqueuedQuery(queryId);

        return queryId;
    }

    public QueryResult getQueryResult(Long queryId) {
        var resultLines = this.resultRepository.findByQueryId(queryId);
        var result = new QueryResult();

        result.setQueryId(queryId);
        result.setTotalNumberOfPatients(
                resultLines.stream().map(Result::getResult).reduce(0, Integer::sum));

        // Sort by display site id, otherwise the real site ids would be easily identifiable by order
        resultLines.sort(Comparator.comparingInt(Result::getDisplaySiteId));

        resultLines.forEach(
                line -> {
                    var resultLine = new QueryResultLine();
                    resultLine.setNumberOfPatients(line.getResult());
                    resultLine.setSiteName(line.getDisplaySiteId().toString());
                    result.getResultLines().add(resultLine);
                });

        return result;
    }
}
