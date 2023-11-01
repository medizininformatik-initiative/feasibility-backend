package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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

@Tag("query")
@ExtendWith(MockitoExtension.class)
public class QueryHandlerServiceTest {

    @Mock
    private QueryRepository queryRepository;

    @Mock
    private ResultService resultService;

    @Mock
    private QueryDispatcher queryDispatcher;

    @Mock
    private QueryTemplateHandler queryTemplateHandler;

    @Mock
    private QueryContentRepository queryContentRepository;

    @Mock
    private QueryTemplateRepository queryTemplateRepository;

    @Mock
    private SavedQueryRepository savedQueryRepository;

    @Spy
    private ObjectMapper jsonUtil = new ObjectMapper();

    private QueryHandlerService createQueryHandlerService() {
        return new QueryHandlerService(queryDispatcher, queryTemplateHandler, queryRepository, queryContentRepository,
                resultService, queryTemplateRepository, savedQueryRepository, jsonUtil);
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(queryDispatcher, queryTemplateHandler, queryRepository, queryContentRepository,
                resultService, queryTemplateRepository, savedQueryRepository, jsonUtil);
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
