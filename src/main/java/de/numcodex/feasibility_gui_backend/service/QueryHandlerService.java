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
import de.numcodex.feasibility_gui_backend.service.query_executor.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
public class QueryHandlerService {

  // TODO: Find correct media types
  private static final String MEDIA_TYPE_CQL = "text/cql";
  private static final String MEDIA_TYPE_FHIR = "FHIR";

  private static final String UNKNOWN_SITE = "Unbekannter Standort";

  private final ObjectMapper objectMapper;

  private final QueryRepository queryRepository;
  private final ResultRepository resultRepository;
  private final BrokerClient brokerClient;
  private final QueryStatusListener queryStatusListener;

  private final QueryBuilder cqlQueryBuilder;

  private boolean brokerQueryStatusListenerConfigured;

  public QueryHandlerService(QueryRepository queryRepository, ResultRepository resultRepository,
                             @Qualifier("applied") BrokerClient brokerClient,
                             ObjectMapper objectMapper, QueryStatusListener queryStatusListener,
                             @Qualifier("cql") QueryBuilder cqlQueryBuilder) {
    this.queryRepository = Objects.requireNonNull(queryRepository);
    this.resultRepository = Objects.requireNonNull(resultRepository);
    this.brokerClient = Objects.requireNonNull(brokerClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.queryStatusListener = Objects.requireNonNull(queryStatusListener);
    this.cqlQueryBuilder = Objects.requireNonNull(cqlQueryBuilder);
    brokerQueryStatusListenerConfigured = false;
  }

  public String runQuery(StructuredQuery structuredQuery)
          throws UnsupportedMediaTypeException, QueryNotFoundException, IOException, QueryBuilderException {

    // TODO: maybe do this using a post construct method (think about middleware availability on startup + potential backoff!)
    if (!brokerQueryStatusListenerConfigured) {
        brokerClient.addQueryStatusListener(queryStatusListener);
        brokerQueryStatusListenerConfigured = true;
    }

    var queryId = this.brokerClient.createQuery();
    var query = new Query();
    query.setQueryId(queryId);

    String structuredQueryStr = objectMapper.writeValueAsString(structuredQuery);
    query.setStructuredQuery(objectMapper.readTree(structuredQueryStr));

    var cqlContent = cqlQueryBuilder.getQueryContent(structuredQuery);
    this.brokerClient.addQueryDefinition(queryId, MEDIA_TYPE_CQL, cqlContent);
    query.getContents().put(MEDIA_TYPE_CQL, cqlContent);

//    String fhirContent = getFhirContent();
//    this.brokerClient.addQueryDefinition(queryId, MEDIA_TYPE_FHIR, fhirContent);
//    query.getContents().put(MEDIA_TYPE_FHIR, fhirContent);

    this.brokerClient.publishQuery(queryId);
    this.queryRepository.save(query);

    return queryId;
  }

  // TODO: implement using QueryBuilderFhir
  private String getFhirContent() {
    // return getQueryContent(...);
    return "FHIR Search query";
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
