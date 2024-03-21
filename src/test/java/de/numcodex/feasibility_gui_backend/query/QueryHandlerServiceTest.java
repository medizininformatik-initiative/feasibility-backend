package de.numcodex.feasibility_gui_backend.query;

import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.QueryListEntry;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateHandler;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class QueryHandlerServiceTest {

    private static final long QUERY_ID = 1L;
    private static final long RESULT_SIZE = 150L;
    private static final String CREATOR = "author-123";
    private static final String QUERY_CONTENT_HASH = "c85f4c5c8e22275b6efcf41f4f3b6d4b";
    private static final String LABEL = "label";
    private static final String COMMENT = "comment";
    public static final String LAST_MODIFIED_STRING = "1969-07-20 20:17:40.0";
    private static final Timestamp LAST_MODIFIED = Timestamp.valueOf(LAST_MODIFIED_STRING);

    @Spy
    private ObjectMapper jsonUtil = new ObjectMapper();

    @Mock
    private QueryDispatcher queryDispatcher;

    @Mock
    private QueryTemplateHandler queryTemplateHandler;

    @Mock
    private QueryRepository queryRepository;

    @Mock
    private QueryContentRepository queryContentRepository;

    @Mock
    private ResultService resultService;

    @Mock
    private QueryTemplateRepository queryTemplateRepository;

    @Mock
    private SavedQueryRepository savedQueryRepository;

    @Mock
    private StructuredQueryValidation structuredQueryValidation;

    private QueryHandlerService queryHandlerService;

    private QueryHandlerService createQueryHandlerService() {
        return new QueryHandlerService(queryDispatcher, queryTemplateHandler, queryRepository, queryContentRepository,
                resultService, queryTemplateRepository, savedQueryRepository, structuredQueryValidation, jsonUtil);
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(queryDispatcher, queryTemplateHandler, queryRepository, queryContentRepository,
            resultService, queryTemplateRepository, savedQueryRepository, jsonUtil);
        queryHandlerService = createQueryHandlerService();
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

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void convertQueriesToQueryListEntries(String withSavedQuery, String skipValidation) throws JsonProcessingException {
        var queryList = List.of(createQuery(Boolean.parseBoolean(withSavedQuery)));
        if (!Boolean.parseBoolean(skipValidation)) {
            doReturn(
                List.of(createInvalidCriterion())
            ).when(structuredQueryValidation).getInvalidCriteria(any(StructuredQuery.class));
        }

        List<QueryListEntry> queryListEntries = queryHandlerService.convertQueriesToQueryListEntries(queryList, Boolean.parseBoolean(skipValidation));

        assertThat(queryListEntries.size()).isEqualTo(1);
        assertThat(queryListEntries.get(0).id()).isEqualTo(QUERY_ID);
        assertThat(queryListEntries.get(0).createdAt()).isEqualTo(LAST_MODIFIED);
        if (Boolean.parseBoolean(withSavedQuery)) {
            assertThat(queryListEntries.get(0).label()).isEqualTo(LABEL);
            assertThat(queryListEntries.get(0).totalNumberOfPatients()).isEqualTo(RESULT_SIZE);
        }
        if (Boolean.parseBoolean(skipValidation)) {
            assertThat(queryListEntries.get(0).isValid()).isNull();
        } else {
            assertThat(queryListEntries.get(0).isValid()).isNotNull();
        }
    }

    @Test
    void convertQueriesToQueryListEntries_JsonProcessingExceptionCausesInvalidQuery() throws JsonProcessingException {
        var queryList = List.of(createQuery(false));
        doThrow(JsonProcessingException.class).when(jsonUtil).readValue(any(String.class), any(Class.class));

        List<QueryListEntry> queryListEntries = queryHandlerService.convertQueriesToQueryListEntries(queryList, false);

        assertThat(queryListEntries.size()).isEqualTo(1);
        assertThat(queryListEntries.get(0).id()).isEqualTo(QUERY_ID);
        assertThat(queryListEntries.get(0).createdAt()).isEqualTo(LAST_MODIFIED);
        assertThat(queryListEntries.get(0).isValid()).isFalse();
    }

    private Query createQuery(boolean withSavedQuery) throws JsonProcessingException {
        var query = new Query();
        query.setId(QUERY_ID);
        query.setCreatedAt(LAST_MODIFIED);
        query.setCreatedBy(CREATOR);
        query.setQueryContent(createQueryContent());
        if (withSavedQuery) {
            query.setSavedQuery(createSavedQuery());
        }
        return query;
    }

    private SavedQuery createSavedQuery() {
        var savedQuery = new SavedQuery();
        savedQuery.setId(0L);
        savedQuery.setLabel(LABEL);
        savedQuery.setComment(COMMENT);
        savedQuery.setResultSize(RESULT_SIZE);
        return savedQuery;
    }

    private QueryContent createQueryContent() throws JsonProcessingException {
        var queryContentString = jsonUtil.writeValueAsString(createValidStructuredQuery());
        var queryContentHash = QUERY_CONTENT_HASH;
        var queryContent = new QueryContent(queryContentString);
        queryContent.setHash(queryContentHash);
        return queryContent;
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

    private Criterion createInvalidCriterion() {
        var termCode = TermCode.builder()
            .code("LL2191-6")
            .system("http://loinc.org")
            .display("Geschlecht")
            .build();
        return Criterion.builder()
            .context(null)
            .termCodes(List.of(termCode))
            .build();
    }
}
