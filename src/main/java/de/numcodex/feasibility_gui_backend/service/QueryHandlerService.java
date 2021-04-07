package de.numcodex.feasibility_gui_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.db.Query;
import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.QueryResultLine;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilder;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilderException;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListenerImpl;
import de.numcodex.feasibility_gui_backend.service.query_executor.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import java.io.IOException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QueryHandlerService {

  // TODO: Find correct media types
  private static final String MEDIA_TYPE_STRUCT_QUERY = "text/structured-query";
  private static final String MEDIA_TYPE_CQL = "text/cql";
  private static final String MEDIA_TYPE_FHIR = "text/fhir-codex";


  private static final String UNKNOWN_SITE = "Unbekannter Standort";

  private final ObjectMapper objectMapper;

  private final QueryRepository queryRepository;
  private final ResultRepository resultRepository;
  private final BrokerClient brokerClient;
  private final QueryStatusListener queryStatusListener;
  private final QueryBuilder cqlQueryBuilder;
  private final QueryBuilder fhirQueryBuilder;
  private final boolean fhirTranslateEnabled;
  private final boolean cqlTranslateEnabled;

  private boolean brokerQueryStatusListenerConfigured;


  public QueryHandlerService(QueryRepository queryRepository, ResultRepository resultRepository,
      @Qualifier("applied") BrokerClient brokerClient,
      ObjectMapper objectMapper, QueryStatusListener queryStatusListener,
      @Qualifier("cql") QueryBuilder cqlQueryBuilder,
      @Qualifier("fhir") QueryBuilder fhirQueryBuilder,
      @Value("${app.fhirTranslationEnabled}") boolean fhirTranslateEnabled,
      @Value("${app.cqlTranslationEnabled}") boolean cqlTranslateEnabled) {
    this.queryRepository = Objects.requireNonNull(queryRepository);
    this.resultRepository = Objects.requireNonNull(resultRepository);
    this.brokerClient = Objects.requireNonNull(brokerClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
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
    var query = createQuery();
    addQueryContent(structuredQuery, query);
    sendQuery(query);
    this.queryRepository.save(query);

    return query.getQueryId();
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
    query.setQueryId(queryId);
    return query;
  }

  private void sendQuery(Query query)
      throws QueryNotFoundException, UnsupportedMediaTypeException, IOException {
    for (var entry : query.getContents().entrySet()) {
      this.brokerClient.addQueryDefinition(query.getQueryId(), entry.getKey(), entry.getValue());
    }
    this.brokerClient.publishQuery(query.getQueryId());
  }

  private void addQueryContent(StructuredQuery structuredQuery, Query query)
      throws IOException, QueryBuilderException {
    addSqQuery(query, structuredQuery);
    if (cqlTranslateEnabled) {
      addCqlQuery(query, structuredQuery);
    }
    if (fhirTranslateEnabled) {
      addFhirQuery(query, structuredQuery);
    }
  }

  private void addSqQuery(Query query, StructuredQuery structuredQuery)
      throws IOException {
    var sqContent = objectMapper.writeValueAsString(structuredQuery);
    query.getContents().put(MEDIA_TYPE_STRUCT_QUERY, sqContent);
  }

  private void addFhirQuery(Query query, StructuredQuery structuredQuery)
      throws QueryBuilderException {
    var fhirContent = getFhirContent(structuredQuery);
    query.getContents().put(MEDIA_TYPE_FHIR, fhirContent);
  }

  private void addCqlQuery(Query query, StructuredQuery structuredQuery)
      throws QueryBuilderException {
    var cqlContent = getCqlContent(structuredQuery);
    query.getContents().put(MEDIA_TYPE_CQL, cqlContent);
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
        resultLines.stream().map(Result::getNumberOfPatients).reduce(0, Integer::sum));

    resultLines.forEach(
        line -> {
          var resultLine = new QueryResultLine();
          resultLine.setNumberOfPatients(line.getNumberOfPatients());
          try {
            resultLine.setSiteName(brokerClient.getSiteName(line.getSiteId()));
          } catch (SiteNotFoundException | IOException e) {
            resultLine.setSiteName(UNKNOWN_SITE);
          }

          result.getResultLines().add(resultLine);
        });

    return result;
  }
}
