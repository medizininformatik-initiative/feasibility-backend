package de.numcodex.feasibility_gui_backend.service;

import static de.numcodex.feasibility_gui_backend.model.db.QueryStatus.PUBLISHED;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.CQL;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.FHIR;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.STRUCTURED_QUERY;
import static de.numcodex.feasibility_gui_backend.service.ServiceSpringConfig.throwingConsumerWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.ThrowingConsumer;
import de.numcodex.feasibility_gui_backend.model.db.Query;
import de.numcodex.feasibility_gui_backend.model.db.QueryContent;
import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.QueryResultLine;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.repository.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.repository.SiteRepository;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilder;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilderException;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import de.numcodex.feasibility_gui_backend.service.query_executor.impl.aktin.AktinBrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.impl.direct.DirectBrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.impl.dsf.DSFBrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.impl.mock.MockBrokerClient;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QueryHandlerService {
    private final ObjectMapper objectMapper;
    private final MessageDigest md5MessageDigest;

    private final QueryRepository queryRepository;
    private final QueryContentRepository queryContentRepository;
    private final ResultRepository resultRepository;
    private final SiteRepository siteRepository;
    private final List<BrokerClient> brokerClients;
    private final List<QueryStatusListener> queryStatusListeners;
    private final QueryBuilder cqlQueryBuilder;
    private final QueryBuilder fhirQueryBuilder;
    private final boolean fhirTranslateEnabled;
    private final boolean cqlTranslateEnabled;

    private boolean brokerQueryStatusListenerConfigured;


    public QueryHandlerService(QueryRepository queryRepository,
        QueryContentRepository queryContentRepository,
        ResultRepository resultRepository,
        SiteRepository siteRepository,
        @Qualifier("applied") List<BrokerClient> brokerClients,
        ObjectMapper objectMapper,
        @Qualifier("md5") MessageDigest md5MessageDigest,
        List<QueryStatusListener> queryStatusListeners,
        @Qualifier("cql") QueryBuilder cqlQueryBuilder,
        @Qualifier("fhir") QueryBuilder fhirQueryBuilder,
        @Value("${app.fhirTranslationEnabled}") boolean fhirTranslateEnabled,
        @Value("${app.cqlTranslationEnabled}") boolean cqlTranslateEnabled) {

        this.queryRepository = Objects.requireNonNull(queryRepository);
        this.queryContentRepository = Objects.requireNonNull(queryContentRepository);
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.siteRepository = Objects.requireNonNull(siteRepository);
        this.brokerClients = Objects.requireNonNull(brokerClients);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.md5MessageDigest = Objects.requireNonNull(md5MessageDigest);
        this.queryStatusListeners = Objects.requireNonNull(queryStatusListeners);
        this.cqlQueryBuilder = Objects.requireNonNull(cqlQueryBuilder);
        this.fhirQueryBuilder = Objects.requireNonNull(fhirQueryBuilder);
        brokerQueryStatusListenerConfigured = false;
        this.fhirTranslateEnabled = fhirTranslateEnabled;
        this.cqlTranslateEnabled = cqlTranslateEnabled;
    }

    public Long runQuery(StructuredQuery structuredQuery)
            throws UnsupportedMediaTypeException, QueryNotFoundException, IOException, QueryBuilderException {

        var sq = objectMapper.writeValueAsString(structuredQuery);

        var hash = new String(md5MessageDigest.digest(sq.getBytes())).replaceAll("\\s", "").replaceAll("\u0000", "");
        System.out.println("HASH: " + hash);

        var queryContent =
                queryContentRepository.findByHash(hash)
                        .orElseGet(() -> {
                            var qc = new QueryContent(sq);
                            qc.setHash(hash);
                            queryContentRepository.save(qc);
                            return qc;
                        });

        var query = createQuery();
        query.setQueryContent(queryContent);

        addQueryContent(structuredQuery, query);
        sendQuery(query);
        queryRepository.save(query);
        return query.getId();
    }

    private Query createQuery() throws IOException {
        var query = new Query();
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
        query.setStatus(PUBLISHED);
        return query;
    }

    private void sendQuery(Query query) throws QueryNotFoundException, IOException {
        brokerClients.forEach(throwingConsumerWrapper(bc -> {
            switch (bc.getClass().getSimpleName()) {
                case "DirectBrokerClient":
                    bc.publishQuery(query.getDirectId());
                    break;
                case "AktinBrokerClient":
                    bc.publishQuery(query.getAktinId());
                    break;
                case "DSFBrokerClient":
                    bc.publishQuery(query.getDsfId());
                    break;
                case "MockBrokerClient":
                default:
                    bc.publishQuery(query.getMockId());
                    break;
            }
        }));
    }

    private void addQueryContent(StructuredQuery structuredQuery, Query query)
            throws IOException, QueryBuilderException, UnsupportedMediaTypeException, QueryNotFoundException {
        addSqQuery(query, structuredQuery);
        if (cqlTranslateEnabled) {
            addCqlQuery(query, structuredQuery);
        }
        if (fhirTranslateEnabled) {
            addFhirQuery(query, structuredQuery);
        }
    }

    private void addSqQuery(Query query, StructuredQuery structuredQuery)
            throws IOException, UnsupportedMediaTypeException, QueryNotFoundException {
        var sqContent = objectMapper.writeValueAsString(structuredQuery);
        brokerClients.forEach(throwingConsumerWrapper(bc -> {
            switch (bc.getClass().getSimpleName()) {
                case "DirectBrokerClient":
                    bc.addQueryDefinition(query.getDirectId(), STRUCTURED_QUERY, sqContent);
                    break;
                case "AktinBrokerClient":
                    bc.addQueryDefinition(query.getAktinId(), STRUCTURED_QUERY, sqContent);
                    break;
                case "DSFBrokerClient":
                    bc.addQueryDefinition(query.getDsfId(), STRUCTURED_QUERY, sqContent);
                    break;
                case "MockBrokerClient":
                default:
                    bc.addQueryDefinition(query.getMockId(), STRUCTURED_QUERY, sqContent);
                    break;
            }

        }));
    }

    private void addFhirQuery(Query query, StructuredQuery structuredQuery)
            throws QueryBuilderException, UnsupportedMediaTypeException, QueryNotFoundException, IOException {
        var fhirContent = getFhirContent(structuredQuery);
        // TODO: Depending on how the issue with multiple query ids is solved, this needs to be fixed
        brokerClients.forEach(throwingConsumerWrapper(bc -> {
            switch (bc.getClass().getSimpleName()) {
                case "DirectBrokerClient":
                    bc.addQueryDefinition(query.getDirectId(), FHIR, fhirContent);
                    break;
                case "AktinBrokerClient":
                    bc.addQueryDefinition(query.getAktinId(), FHIR, fhirContent);
                    break;
                case "DSFBrokerClient":
                    bc.addQueryDefinition(query.getDsfId(), FHIR, fhirContent);
                    break;
                case "MockBrokerClient":
                default:
                    bc.addQueryDefinition(query.getMockId(), FHIR, fhirContent);
                    break;
            }
        }));
    }

    private void addCqlQuery(Query query, StructuredQuery structuredQuery)
            throws QueryBuilderException, UnsupportedMediaTypeException, QueryNotFoundException, IOException {
        var cqlContent = getCqlContent(structuredQuery);
        // TODO: Depending on how the issue with multiple query ids is solved, this needs to be fixed
        brokerClients.forEach(throwingConsumerWrapper(bc -> {
            switch (bc.getClass().getSimpleName()) {
                case "DirectBrokerClient":
                    bc.addQueryDefinition(query.getDirectId(), CQL, cqlContent);
                    break;
                case "AktinBrokerClient":
                    bc.addQueryDefinition(query.getAktinId(), CQL, cqlContent);
                    break;
                case "DSFBrokerClient":
                    bc.addQueryDefinition(query.getDsfId(), CQL, cqlContent);
                    break;
                case "MockBrokerClient":
                default:
                    bc.addQueryDefinition(query.getMockId(), CQL, cqlContent);
                    break;
            }
        }));
    }

    private String getFhirContent(StructuredQuery structuredQuery) throws QueryBuilderException {
        return this.fhirQueryBuilder.getQueryContent(structuredQuery);
    }

    private String getCqlContent(StructuredQuery structuredQuery) throws QueryBuilderException {
        return this.cqlQueryBuilder.getQueryContent(structuredQuery);
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
