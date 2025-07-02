package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.query.persistence.QueryRepository;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslator;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class QueryHandlerServiceTest {


  @Spy
  private ObjectMapper jsonUtil = new ObjectMapper();

  @Mock
  private QueryDispatcher queryDispatcher;

  @Mock
  private QueryRepository queryRepository;

  @Mock
  private QueryContentRepository queryContentRepository;

  @Mock
  private ResultService resultService;

  @Mock
  private StructuredQueryValidation structuredQueryValidation;

  @Mock
  private QueryTranslator queryTranslator;

  private QueryHandlerService queryHandlerService;

  private QueryHandlerService createQueryHandlerService() {
    return new QueryHandlerService(queryDispatcher, queryRepository, queryContentRepository,
        resultService, structuredQueryValidation, queryTranslator, jsonUtil);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(queryDispatcher, queryRepository, queryContentRepository,
        resultService, jsonUtil);
    queryHandlerService = createQueryHandlerService();
  }

  @Test
  public void testRunQuery_failsWithMonoErrorOnQueryDispatchException() throws QueryDispatchException {
    var testStructuredQuery = StructuredQuery.builder()
        .inclusionCriteria(List.of(List.of()))
        .exclusionCriteria(List.of(List.of()))
        .build();
    var queryHandlerService = createQueryHandlerService();
    doThrow(QueryDispatchException.class).when(queryDispatcher).enqueueNewQuery(any(StructuredQuery.class), any(String.class));

    StepVerifier.create(queryHandlerService.runQuery(testStructuredQuery, "uerid"))
        .expectError(QueryDispatchException.class)
        .verify();
  }
}
