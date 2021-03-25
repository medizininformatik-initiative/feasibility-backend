package de.numcodex.feasibility_gui_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.service.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilder;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.sq2cql.model.structured_query.Criterion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class QueryHandlerTest {
    public static final String QUERY_ID = "QUERY_ID";
    public static final String STRUCTURED_QUERY_JSON = "structured-query-1212";

    private static final String CQL = "CQL-164012";

    public static final de.numcodex.sq2cql.model.structured_query.StructuredQuery STRUCTURED_QUERY = de.numcodex.sq2cql.model.structured_query.StructuredQuery
            .of(List.of(List.of(Criterion.TRUE)));

    @Mock
    private BrokerClient brokerClient;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    QueryRepository queryRepository;

    @Mock
    QueryBuilder cqlQueryBuilder;

    @InjectMocks
    private QueryHandlerService queryService;

    @Test
    public void runQuery() throws Exception {
        when(brokerClient.createQuery()).thenReturn(QUERY_ID);
        var structuredQuery = new StructuredQuery();
        when(cqlQueryBuilder.getQueryContent(structuredQuery)).thenReturn(CQL);
        var queryId = queryService.runQuery(structuredQuery);

        verify(brokerClient).addQueryDefinition(QUERY_ID, "text/cql", CQL);
        verify(brokerClient).publishQuery(QUERY_ID);

        assertEquals(QUERY_ID, queryId);
    }
}
