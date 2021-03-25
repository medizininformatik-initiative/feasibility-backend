package de.numcodex.feasibility_gui_backend.service.query_builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.sq2cql.PrintContext;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.cql.Library;
import de.numcodex.sq2cql.model.structured_query.Criterion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CqlQueryBuilderTest {

    public static final String STRUCTURED_QUERY_JSON = "structured-query-1212";
    public static final de.numcodex.sq2cql.model.structured_query.StructuredQuery STRUCTURED_QUERY = de.numcodex.sq2cql.model.structured_query.StructuredQuery
            .of(List.of(List.of(Criterion.TRUE)));

    @Mock
    Translator translator;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    private CqlQueryBuilder cqlQueryBuilder;

    @Test
    void getQueryContent() throws Exception {
        var structuredQuery = new StructuredQuery();
        when(objectMapper.writeValueAsString(structuredQuery)).thenReturn(STRUCTURED_QUERY_JSON);
        when(objectMapper.readValue(STRUCTURED_QUERY_JSON,
                de.numcodex.sq2cql.model.structured_query.StructuredQuery.class)).thenReturn(STRUCTURED_QUERY);
        when(translator.toCql(STRUCTURED_QUERY)).thenReturn(Library.of());

        var cql = cqlQueryBuilder.getQueryContent(structuredQuery);

        assertEquals(Library.of().print(PrintContext.ZERO), cql);
    }
}
