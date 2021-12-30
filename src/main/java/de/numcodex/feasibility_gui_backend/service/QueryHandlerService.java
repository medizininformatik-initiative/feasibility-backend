package de.numcodex.feasibility_gui_backend.service;

import de.numcodex.feasibility_gui_backend.model.db.Query;
import de.numcodex.feasibility_gui_backend.model.db.QueryContent;
import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.QueryResultLine;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationComponent;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslationException;
import de.numcodex.feasibility_gui_backend.repository.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.STRUCTURED_QUERY;
import static de.numcodex.feasibility_gui_backend.service.ServiceSpringConfig.throwingConsumerWrapper;

@Service
@Transactional
public class QueryHandlerService {
    private final QueryRepository queryRepository;
    private final QueryContentRepository queryContentRepository;
    private final ResultRepository resultRepository;
    private final List<BrokerClient> brokerClients;
    private final MessageDigest md5MessageDigest;
    private final QueryTranslationComponent queryTranslationComponent;


    public QueryHandlerService(QueryRepository queryRepository,
        QueryContentRepository queryContentRepository,
        ResultRepository resultRepository,
        @Qualifier("applied") List<BrokerClient> brokerClients,
        @Qualifier("md5") MessageDigest md5MessageDigest,
        QueryTranslationComponent queryTranslationComponent) {

        this.queryRepository = Objects.requireNonNull(queryRepository);
        this.queryContentRepository = Objects.requireNonNull(queryContentRepository);
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.brokerClients = Objects.requireNonNull(brokerClients);
        this.md5MessageDigest = Objects.requireNonNull(md5MessageDigest);
        this.queryTranslationComponent = Objects.requireNonNull(queryTranslationComponent);
    }

    public Long runQuery(StructuredQuery structuredQuery)
            throws UnsupportedMediaTypeException, QueryNotFoundException, IOException, QueryTranslationException {

        var queryFormats = queryTranslationComponent.translate(structuredQuery);
        var queryContent = findExistingQueryContentOrCreateNewOne(queryFormats.get(STRUCTURED_QUERY));

        var query = createQuery();
        query.setQueryContent(queryContent);

        addQueryFormats(query, queryFormats);
        sendQuery(query);
        queryRepository.save(query);
        return query.getId();
    }

    private QueryContent findExistingQueryContentOrCreateNewOne(String queryRepresentation) {
        var queryHash = calculateQueryHashMD5(queryRepresentation);

        return queryContentRepository.findByHash(queryHash)
                .orElseGet(() -> {
                    var queryContent = new QueryContent(queryRepresentation);
                    queryContent.setHash(queryHash);
                    queryContentRepository.save(queryContent);
                    return queryContent;
                });
    }

    private String calculateQueryHashMD5(String queryRepresentation) {
        return Stream.of(queryRepresentation)
                .map(String::getBytes)
                .map(md5MessageDigest::digest)
                .map(String::new)
                .map(s -> s.replaceAll("\\s", ""))
                .map(s -> s.replaceAll("\u0000", ""))
                .collect(Collectors.joining());
    }

    private Query createQuery() {
        var query = new Query();
        // TODO: this needs to get refactored ASAP!!!
        brokerClients.forEach(throwingConsumerWrapper(bc -> {
            var queryId = bc.createQuery();
            switch (bc.getClass().getSimpleName()) {
                case "DirectBrokerClient":
                    query.setDirectId(queryId);
                    break;
                case "AktinBrokerClient":
                    query.setAktinId(queryId);
                    break;
                case "DSFBrokerClient":
                    query.setDsfId(queryId);
                    break;
                case "MockBrokerClient":
                default:
                    query.setMockId(queryId);
                    break;
            }
        }));
        return query;
    }

    private void sendQuery(Query query) {
        // TODO: this needs to get refactored ASAP!!!
        brokerClients.forEach(throwingConsumerWrapper(bc -> {
            var queryId = switch (bc.getClass().getSimpleName()) {
                case "DirectBrokerClient":
                    yield query.getDirectId();
                case "AktinBrokerClient":
                    yield query.getAktinId();
                case "DSFBrokerClient":
                    yield query.getDsfId();
                case "MockBrokerClient":
                default:
                    yield query.getMockId();
            };

            bc.publishQuery(queryId);
        }));
    }

    private void addQueryFormats(Query query, Map<QueryMediaType, String> queryFormats) {
        // TODO: this needs to get refactored ASAP!!!
        brokerClients.forEach(throwingConsumerWrapper(bc -> {
            var queryId = switch (bc.getClass().getSimpleName()) {
                case "DirectBrokerClient":
                    yield query.getDirectId();
                case "AktinBrokerClient":
                    yield query.getAktinId();
                case "DSFBrokerClient":
                    yield query.getDsfId();
                case "MockBrokerClient":
                default:
                    yield query.getMockId();
            };

            for (Entry<QueryMediaType, String> queryFormatEntry : queryFormats.entrySet()) {
                bc.addQueryDefinition(queryId, queryFormatEntry.getKey().getRepresentation(),
                        queryFormatEntry.getValue());
            }
        }));
    }

    public QueryResult getQueryResult(Long queryId) {
        var resultLines = this.resultRepository.findByQueryId(queryId);
        var result = new QueryResult();

        result.setQueryId(queryId);
        result.setTotalNumberOfPatients(
                resultLines.stream().map(Result::getResult).reduce(0, Integer::sum));

        // Sort by display site id, otherwise the real site ids would be easily identifiable by order
        Collections.sort(resultLines, Comparator.comparingInt(Result::getDisplaySiteId));

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
