package de.numcodex.feasibility_gui_backend.query.templates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryHashCalculator;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@Tag("query")
@Tag("template")
@Tag("peristence")
@ExtendWith(MockitoExtension.class)
class QueryTemplateHandlerTest {

    public static final String CREATOR = "creator-557261";
    public static final String LABEL = "some-label";
    public static final String COMMENT = "some-comment";
    public static final String TIME_STRING = "1969-07-20 20:17:40.0";
    public static final String QUERY_HASH = "fb4eb37b9125f9356d536d2d90d020ea";

    @Mock
    private QueryHashCalculator queryHashCalculator;

    @Spy
    private ObjectMapper jsonUtil = new ObjectMapper();

    @Mock
    private QueryRepository queryRepository;

    @Mock
    private QueryContentRepository queryContentRepository;

    @Mock
    private QueryTemplateRepository queryTemplateRepository;

    private QueryTemplateHandler createQueryTemplateHandler() {
        return new QueryTemplateHandler(queryHashCalculator, jsonUtil, queryRepository, queryContentRepository, queryTemplateRepository);
    }

    @Test
    @DisplayName("storeNewQuery() -> trying to store a null object throws an exception")
    void storeNewQuery_throwsOnEmptyQuery() {
        var queryTemplateHandler = createQueryTemplateHandler();
        assertThrows(QueryTemplateException.class, () -> queryTemplateHandler.storeNewQuery(null, null));
    }

    @Test
    @DisplayName("storeNewQuery() -> error in json serialization throws an exception")
    void storeNewQuery_throwsOnJsonSerializationError() throws JsonProcessingException {
        var structuredQuery = createValidStructuredQuery();
        doThrow(JsonProcessingException.class).when(jsonUtil).writeValueAsString(any(StructuredQuery.class));

        var queryTemplateHandler = createQueryTemplateHandler();
        assertThrows(QueryTemplateException.class, () -> queryTemplateHandler.storeNewQuery(structuredQuery, CREATOR));
    }

    @Test
    @DisplayName("convertApiToPersistence() -> converting a query from the rest api to the format that will be stored in the database succeeds")
    void convertApiToPersistence() {
        var apiQueryTemplate = createApiQueryTemplate();
        var persistenceQueryTemplate = createPersistenceQueryTemplate();
        doReturn(createQuery()).when(queryRepository).getReferenceById(any(Long.class));

        var queryTemplateHandler = createQueryTemplateHandler();
        var convertedQueryTemplate = queryTemplateHandler.convertApiToPersistence(apiQueryTemplate, 1L);

        assertEquals(convertedQueryTemplate.getId(), null);
        assertEquals(convertedQueryTemplate.getLabel(), persistenceQueryTemplate.getLabel());
        assertEquals(convertedQueryTemplate.getComment(), persistenceQueryTemplate.getComment());
        assertEquals(convertedQueryTemplate.getLastModified(), persistenceQueryTemplate.getLastModified());
        assertEquals(convertedQueryTemplate.getQuery().getCreatedAt(), persistenceQueryTemplate.getQuery().getCreatedAt());
        assertEquals(convertedQueryTemplate.getQuery().getQueryContent().getQueryContent(), persistenceQueryTemplate.getQuery().getQueryContent().getQueryContent());
    }

    @Test
    @DisplayName("convertPersistenceToApi() -> converting a query from the database to the format that will be sent out via api succeeds")
    void convertPersistenceToApi() throws JsonProcessingException {
        var persistenceQueryTemplate = createPersistenceQueryTemplate();
        var apiQueryTemplate = createApiQueryTemplate();

        var queryTemplateHandler = createQueryTemplateHandler();
        var convertedQueryTemplate = queryTemplateHandler.convertPersistenceToApi(persistenceQueryTemplate);

        assertEquals(convertedQueryTemplate.id(), apiQueryTemplate.id());
        assertEquals(convertedQueryTemplate.label(), apiQueryTemplate.label());
        assertEquals(convertedQueryTemplate.comment(), apiQueryTemplate.comment());
        assertEquals(convertedQueryTemplate.createdBy(), apiQueryTemplate.createdBy());
        assertEquals(convertedQueryTemplate.lastModified(), apiQueryTemplate.lastModified());
        assertEquals(convertedQueryTemplate.content().display(), apiQueryTemplate.content().display());
        assertEquals(convertedQueryTemplate.content().version(), apiQueryTemplate.content().version());
        assertEquals(convertedQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).code(),
                apiQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).code());
        assertEquals(convertedQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).display(),
                apiQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).display());
        assertEquals(convertedQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).version(),
                apiQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).version());
        assertEquals(convertedQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).system(),
                apiQueryTemplate.content().inclusionCriteria().get(0).get(0).termCodes().get(0).system());
        assertEquals(convertedQueryTemplate.content().exclusionCriteria(), apiQueryTemplate.content().exclusionCriteria());
    }

    private de.numcodex.feasibility_gui_backend.query.api.QueryTemplate createApiQueryTemplate() {
        return de.numcodex.feasibility_gui_backend.query.api.QueryTemplate.builder()
                .label(LABEL)
                .comment(COMMENT)
                .createdBy(CREATOR)
                .lastModified(TIME_STRING)
                .content(createValidStructuredQuery())
                .id(1L)
                .build();
    }

    private de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate createPersistenceQueryTemplate() {
        var queryTemplate = new de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate();
        queryTemplate.setLabel(LABEL);
        queryTemplate.setComment(COMMENT);
        queryTemplate.setLastModified(Timestamp.valueOf(TIME_STRING));
        queryTemplate.setId(1L);
        queryTemplate.setQuery(createQuery());
        return queryTemplate;
    }

    private Query createQuery() {
        var queryContent = new QueryContent(createValidQueryContentString());
        queryContent.setHash(QUERY_HASH);
        var query = new Query();
        query.setQueryContent(queryContent);
        query.setCreatedBy(CREATOR);
        query.setCreatedAt(Timestamp.valueOf(TIME_STRING));
        return query;
    }

    private StructuredQuery createValidStructuredQuery() {
        var termCode = TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();
        var inclusionCriterion = Criterion.builder()
                .termCodes(List.of(termCode))
                .attributeFilters(List.of())
                .build();
        return StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(inclusionCriterion)))
                .exclusionCriteria(List.of())
                .display("foo")
                .build();
    }

    private String createValidQueryContentString() {
        return "{ \"version\": \"http://to_be_decided.com/draft-2/schema#\", \"display\": \"foo\", \"inclusionCriteria\": [ [ { \"termCodes\": [ { \"code\": \"LL2191-6\", \"display\": \"Geschlecht\", \"system\": \"http://loinc.org\" } ] } ] ], \"exclusionCriteria\": [ ]}";
    }
}
