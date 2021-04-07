package de.numcodex.feasibility_gui_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class QueryHandlerServiceTest {


  @Mock
  private QueryRepository queryRepository;

  @Mock
  private ResultRepository resultRepository;

  @Mock
  private BrokerClient brokerClient;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private QueryStatusListener queryStatusListener;

  @Mock
  private QueryBuilder fhirQueryBuilder;

  @Mock
  private QueryBuilder cqlQueryBuilder;

  private QueryHandlerService queryHandlerService;

  @BeforeEach
  public void setup() {
    queryHandlerService = new QueryHandlerService(queryRepository,
        resultRepository, brokerClient, objectMapper, queryStatusListener, cqlQueryBuilder,
        fhirQueryBuilder, true, true);
  }


  @Test
  public void testRunQuery_addQueryStatusListenerCalled()
      throws IOException, UnsupportedMediaTypeException, QueryNotFoundException, QueryBuilderException {
    var structuredQuery = new StructuredQuery();
    queryHandlerService.runQuery(structuredQuery);
    verify(brokerClient).addQueryStatusListener(queryStatusListener);
  }

  @Test
  public void testRunQuery_addQueryStatusListenerCalledOnlyOnce()
      throws IOException, UnsupportedMediaTypeException, QueryNotFoundException, QueryBuilderException {
    var structuredQuery = new StructuredQuery();
    queryHandlerService.runQuery(structuredQuery);
    queryHandlerService.runQuery(structuredQuery);
    verify(brokerClient, times(1)).addQueryStatusListener(queryStatusListener);
  }

  @Test
  public void testRunQuery_saveQuery()
      throws IOException, UnsupportedMediaTypeException, QueryNotFoundException, QueryBuilderException {
    var structuredQuery = new StructuredQuery();
    var expectedQuery = new Query();
    expectedQuery.setQueryId("42");
    when(objectMapper.writeValueAsString(structuredQuery)).thenReturn("structured_query");
    when(cqlQueryBuilder.getQueryContent(structuredQuery)).thenReturn("cql_query");
    when(fhirQueryBuilder.getQueryContent(structuredQuery)).thenReturn("fhir_query");

    expectedQuery.getContents().put("text/structured-query", "structured_query");
    expectedQuery.getContents().put("text/cql", "cql_query");
    expectedQuery.getContents().put("text/fhir-codex", "fhir_query");
    when(brokerClient.createQuery()).thenReturn("42");
    queryHandlerService.runQuery(structuredQuery);
    verify(queryRepository).save(expectedQuery);
  }

  @Test
  public void testRunQuery_sendQuery()
      throws IOException, UnsupportedMediaTypeException, QueryNotFoundException, QueryBuilderException {
    var structuredQuery = new StructuredQuery();
    var expectedQuery = new Query();
    expectedQuery.setQueryId("42");
    when(objectMapper.writeValueAsString(structuredQuery)).thenReturn("structured_query");
    when(cqlQueryBuilder.getQueryContent(structuredQuery)).thenReturn("cql_query");
    when(fhirQueryBuilder.getQueryContent(structuredQuery)).thenReturn("fhir_query");

    expectedQuery.getContents().put("text/structured-query", "structured_query");
    expectedQuery.getContents().put("text/cql", "cql_query");
    expectedQuery.getContents().put("text/fhir-codex", "fhir_query");
    when(brokerClient.createQuery()).thenReturn("42");
    queryHandlerService.runQuery(structuredQuery);
    for (var entry : expectedQuery.getContents().entrySet()) {
      verify(brokerClient)
          .addQueryDefinition(expectedQuery.getQueryId(), entry.getKey(), entry.getValue());
    }
    verify(brokerClient).publishQuery(expectedQuery.getQueryId());
  }

  @Test
  public void testRunQuery_getQueryId()
      throws IOException, UnsupportedMediaTypeException, QueryNotFoundException, QueryBuilderException {
    var structuredQuery = new StructuredQuery();
    var expectedId = "42";
    when(brokerClient.createQuery()).thenReturn(expectedId);
    var actualId = queryHandlerService.runQuery(structuredQuery);
    assertEquals(expectedId, actualId);
  }

  @Test
  public void testGetQueryResult() throws IOException, SiteNotFoundException {
    var mockResult = new Result();
    mockResult.setNumberOfPatients(5);
    mockResult.setSiteId("LübeckId");
    mockResult.setQueryId("42");
    when(resultRepository.findByQueryId("42")).thenReturn(List.of(mockResult));
    when(brokerClient.getSiteName("LübeckId")).thenReturn("Lübeck");
    var expectedQueryResult = new QueryResult();
    expectedQueryResult.setQueryId("42");
    expectedQueryResult.setTotalNumberOfPatients(5);
    var expectedResultLine = new QueryResultLine();
    expectedQueryResult.setQueryId("42");
    expectedResultLine.setSiteName("Lübeck");
    expectedResultLine.setNumberOfPatients(5);
    expectedQueryResult.getResultLines().add(expectedResultLine);

    var queryResult = queryHandlerService.getQueryResult("42");
    assertEquals(expectedQueryResult, queryResult);
  }

  @Test
  public void testGetQueryResult_unknownSite() throws IOException, SiteNotFoundException {
    var mockResult = new Result();
    mockResult.setNumberOfPatients(5);
    mockResult.setSiteId("LübeckId");
    mockResult.setQueryId("42");
    when(resultRepository.findByQueryId("42")).thenReturn(List.of(mockResult));
    when(brokerClient.getSiteName("LübeckId")).thenThrow(SiteNotFoundException.class);

    var queryResult = queryHandlerService.getQueryResult("42");
    assertEquals("Unbekannter Standort", queryResult.getResultLines().get(0).getSiteName());
  }

  @Test
  public void testGetQueryResult_sum() {
    var mockResult = new Result();
    mockResult.setNumberOfPatients(5);
    var mockResult2 = new Result();
    mockResult2.setNumberOfPatients(14);
    when(resultRepository.findByQueryId("42")).thenReturn(List.of(mockResult, mockResult2));
    var queryResult = queryHandlerService.getQueryResult("42");
    assertEquals(19, queryResult.getTotalNumberOfPatients());
  }
}
