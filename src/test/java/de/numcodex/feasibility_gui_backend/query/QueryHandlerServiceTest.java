package de.numcodex.feasibility_gui_backend.query;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private QueryHandlerService queryHandlerService;

    private QueryHandlerService createQueryHandlerService() {
        return new QueryHandlerService(queryDispatcher, queryTemplateHandler, queryRepository, queryContentRepository,
                resultService, queryTemplateRepository, savedQueryRepository, jsonUtil);
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(queryDispatcher, queryTemplateHandler, queryRepository, queryContentRepository,
                resultService, queryTemplateRepository, savedQueryRepository, jsonUtil);
        queryHandlerService = createQueryHandlerService();
    }

    @Test
    void convertQueriesToQueryListEntries_withoutSavedQuery() throws JsonProcessingException {
        var queryList = List.of(createQuery(false));

        List<QueryListEntry> queryListEntries = queryHandlerService.convertQueriesToQueryListEntries(queryList);

        assertThat(queryListEntries.size()).isEqualTo(1);
        assertThat(queryListEntries.get(0).id()).isEqualTo(QUERY_ID);
        assertThat(queryListEntries.get(0).createdAt()).isEqualTo(LAST_MODIFIED);
    }

    @Test
    void convertQueriesToQueryListEntries_withSavedQuery() throws JsonProcessingException {
        var queryList = List.of(createQuery(true));

        List<QueryListEntry> queryListEntries = queryHandlerService.convertQueriesToQueryListEntries(queryList);

        assertThat(queryListEntries.size()).isEqualTo(1);
        assertThat(queryListEntries.get(0).id()).isEqualTo(QUERY_ID);
        assertThat(queryListEntries.get(0).createdAt()).isEqualTo(LAST_MODIFIED);
        assertThat(queryListEntries.get(0).label()).isEqualTo(LABEL);
        assertThat(queryListEntries.get(0).totalNumberOfPatients()).isEqualTo(RESULT_SIZE);
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
}
