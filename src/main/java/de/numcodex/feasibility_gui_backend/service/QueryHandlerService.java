package de.numcodex.feasibility_gui_backend.service;

import static de.numcodex.feasibility_gui_backend.model.db.QueryStatus.ACTIVE;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.CQL;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.FHIR;
import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.STRUCTURED_QUERY;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.db.Query;
import de.numcodex.feasibility_gui_backend.model.db.QueryContent;
import de.numcodex.feasibility_gui_backend.model.db.QuerySite;
import de.numcodex.feasibility_gui_backend.model.db.QuerySite.QuerySiteId;
import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.QueryResultLine;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.repository.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.QuerySiteRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.repository.SiteRepository;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilder;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilderException;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
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
    private final QuerySiteRepository querySiteRepository;
    private final SiteRepository siteRepository;
    private final BrokerClient brokerClient;
    private final QueryStatusListener queryStatusListener;
    private final QueryBuilder cqlQueryBuilder;
    private final QueryBuilder fhirQueryBuilder;
    private final boolean fhirTranslateEnabled;
    private final boolean cqlTranslateEnabled;

    private boolean brokerQueryStatusListenerConfigured;


    public QueryHandlerService(QueryRepository queryRepository,
        QueryContentRepository queryContentRepository,
        ResultRepository resultRepository,
        QuerySiteRepository querySiteRepository,
        SiteRepository siteRepository,
        @Qualifier("applied") BrokerClient brokerClient,
        ObjectMapper objectMapper,
        @Qualifier("md5") MessageDigest md5MessageDigest,
        QueryStatusListener queryStatusListener,
        @Qualifier("cql") QueryBuilder cqlQueryBuilder,
        @Qualifier("fhir") QueryBuilder fhirQueryBuilder,
        @Value("${app.fhirTranslationEnabled}") boolean fhirTranslateEnabled,
        @Value("${app.cqlTranslationEnabled}") boolean cqlTranslateEnabled) {

        this.queryRepository = Objects.requireNonNull(queryRepository);
        this.queryContentRepository = Objects.requireNonNull(queryContentRepository);
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.querySiteRepository = Objects.requireNonNull(querySiteRepository);
        this.siteRepository = Objects.requireNonNull(siteRepository);
        this.brokerClient = Objects.requireNonNull(brokerClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.md5MessageDigest = Objects.requireNonNull(md5MessageDigest);
        this.queryStatusListener = Objects.requireNonNull(queryStatusListener);
        this.cqlQueryBuilder = Objects.requireNonNull(cqlQueryBuilder);
        this.fhirQueryBuilder = Objects.requireNonNull(fhirQueryBuilder);
        brokerQueryStatusListenerConfigured = false;
        this.fhirTranslateEnabled = fhirTranslateEnabled;
        this.cqlTranslateEnabled = cqlTranslateEnabled;
    }

    public String runQuery(StructuredQuery structuredQuery)
            throws UnsupportedMediaTypeException, QueryNotFoundException, IOException, QueryBuilderException {

        //TODO: Should not be here
        addQueryStatusListener();

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

        addQueryContent(structuredQuery, query.getId());
        sendQuery(query);
        queryRepository.save(query);

        var sites = siteRepository.findAll();
        sites.forEach(s -> {
            QuerySite qs = new QuerySite();
            QuerySiteId qsId = new QuerySiteId();
            qsId.setQueryId(query.getId());
            qsId.setSiteId(s.getId());
            qs.setId(qsId);
            querySiteRepository.save(qs);
        });
        return query.getId();
    }

    // TODO: maybe do this using a post construct method (think about middleware availability on startup + potential backoff!)
    private void addQueryStatusListener() throws IOException {
        if (!brokerQueryStatusListenerConfigured) {
            brokerClient.addQueryStatusListener(queryStatusListener);
            brokerQueryStatusListenerConfigured = true;
        }
    }

    private Query createQuery() throws IOException {
        var queryId = this.brokerClient.createQuery();
        var query = new Query();
        query.setId(queryId);
        query.setStatus(ACTIVE);
        return query;
    }

    private void sendQuery(Query query) throws QueryNotFoundException, IOException {
        this.brokerClient.publishQuery(query.getId());
    }

    private void addQueryContent(StructuredQuery structuredQuery, String queryId)
            throws IOException, QueryBuilderException, UnsupportedMediaTypeException, QueryNotFoundException {
        addSqQuery(queryId, structuredQuery);
        if (cqlTranslateEnabled) {
            addCqlQuery(queryId, structuredQuery);
        }
        if (fhirTranslateEnabled) {
            addFhirQuery(queryId, structuredQuery);
        }
    }

    private void addSqQuery(String queryId, StructuredQuery structuredQuery)
            throws IOException, UnsupportedMediaTypeException, QueryNotFoundException {
        var sqContent = objectMapper.writeValueAsString(structuredQuery);
        brokerClient.addQueryDefinition(queryId, STRUCTURED_QUERY, sqContent);
    }

    private void addFhirQuery(String queryId, StructuredQuery structuredQuery)
            throws QueryBuilderException, UnsupportedMediaTypeException, QueryNotFoundException, IOException {
        var fhirContent = getFhirContent(structuredQuery);
        brokerClient.addQueryDefinition(queryId, FHIR, fhirContent);
    }

    private void addCqlQuery(String queryId, StructuredQuery structuredQuery)
            throws QueryBuilderException, UnsupportedMediaTypeException, QueryNotFoundException, IOException {
        var cqlContent = getCqlContent(structuredQuery);
        brokerClient.addQueryDefinition(queryId, CQL, cqlContent);
    }

    private String getFhirContent(StructuredQuery structuredQuery) throws QueryBuilderException {
        return this.fhirQueryBuilder.getQueryContent(structuredQuery);
    }

    private String getCqlContent(StructuredQuery structuredQuery) throws QueryBuilderException {
        return this.cqlQueryBuilder.getQueryContent(structuredQuery);
    }

    public QueryResult getQueryResult(String queryId) {
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
