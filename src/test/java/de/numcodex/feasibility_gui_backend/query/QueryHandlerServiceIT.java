package de.numcodex.feasibility_gui_backend.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultLine;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.SavedQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerSpringConfig;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryHashCalculator;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.result.ResultServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateHandler;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.DETAILED;
import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.DETAILED_OBFUSCATED;
import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.SUMMARY;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("handler")
@Import({
        BrokerSpringConfig.class,
        QueryTranslatorSpringConfig.class,
        QueryDispatchSpringConfig.class,
        QueryCollectSpringConfig.class,
        QueryHandlerService.class,
        QueryTemplateHandler.class,
        ResultServiceSpringConfig.class
})
@DataJpaTest(
        properties = {
                "app.cqlTranslationEnabled=false",
                "app.fhirTranslationEnabled=false",
                "app.broker.mock.enabled=true",
                "app.broker.direct.enabled=false",
                "app.broker.aktin.enabled=false",
                "app.broker.dsf.enabled=false"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class QueryHandlerServiceIT {

    public static final String SITE_NAME_1 = "site-name-114606";
    public static final String SITE_NAME_2 = "site-name-114610";
    public static final String CREATOR = "creator-114634";
    public static final long UNKNOWN_QUERY_ID = 9999999L;
    public static final String LABEL = "some-label";
    public static final String COMMENT = "some-comment";
    public static final String TIME_STRING = "1969-07-20 20:17:40.0";

    @Autowired
    private QueryHandlerService queryHandlerService;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private SavedQueryRepository savedQueryRepository;

    @Autowired
    private QueryDispatchRepository queryDispatchRepository;

    @Autowired
    private ResultService resultService;

    @Autowired
    private QueryHashCalculator queryHashCalculator;

    @Autowired
    @Qualifier("translation")
    private ObjectMapper jsonUtil;

    @Test
    public void testRunQuery() {
        var testStructuredQuery = StructuredQuery.builder()
                .inclusionCriteria(List.of(List.of()))
                .exclusionCriteria(List.of(List.of()))
                .build();

        queryHandlerService.runQuery(testStructuredQuery, "test").block();

        assertThat(queryRepository.count()).isOne();
        assertThat(queryDispatchRepository.count()).isOne();
    }

    // This behavior seems to be necessary since the UI is polling constantly.
    // If the response was an error then the UI would need to handle it accordingly.
    // TODO: We should discuss this with the UI team. Maybe a better solution can be identified.
    @Test
    public void testGetQueryResult_UnknownQueryIdLeadsToResultWithZeroMatchesInPopulation() {
        var queryResult = queryHandlerService.getQueryResult(UNKNOWN_QUERY_ID, DETAILED_OBFUSCATED);

        assertThat(queryResult.queryId()).isEqualTo(UNKNOWN_QUERY_ID);
        assertThat(queryResult.totalNumberOfPatients()).isZero();
        assertThat(queryResult.resultLines()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource
    public void testGetQueryResult_ErrorResultsAreIgnored(ResultDetail resultDetail) {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(ERROR)
                        .result(0L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, resultDetail);

        assertThat(queryResult.resultLines()).isEmpty();
    }

    @Test
    public void testGetQueryResult_SummaryContainsOnlyTheTotal() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(SUCCESS)
                        .result(10L)
                        .build());
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_2)
                        .type(SUCCESS)
                        .result(20L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, SUMMARY);

        assertThat(queryResult.totalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.resultLines()).isEmpty();
    }

    @Test
    public void testGetQueryResult_DetailedObfuscatedDoesNotContainTheSiteNames() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(SUCCESS)
                        .result(10L)
                        .build()
        );
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_2)
                        .type(SUCCESS)
                        .result(20L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, DETAILED_OBFUSCATED);

        assertThat(queryResult.totalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.resultLines()).hasSize(2);
        assertThat(queryResult.resultLines().stream().map(QueryResultLine::siteName))
            .doesNotContain(SITE_NAME_1, SITE_NAME_2);
        assertThat(queryResult.resultLines().stream().map(QueryResultLine::numberOfPatients))
            .contains(10L, 20L);
    }

    @Test
    public void testGetQueryResult_DetailedContainsTheSiteNames() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_1)
                        .type(SUCCESS)
                        .result(10L)
                        .build()
        );
        resultService.addResultLine(query.getId(),
                ResultLine.builder()
                        .siteName(SITE_NAME_2)
                        .type(SUCCESS)
                        .result(20L)
                        .build()
        );

        var queryResult = queryHandlerService.getQueryResult(queryId, DETAILED);

        assertThat(queryResult.totalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.resultLines())
            .hasSize(2)
            .contains(QueryResultLine.builder().siteName(SITE_NAME_1).numberOfPatients(10L).build(),
                QueryResultLine.builder().siteName(SITE_NAME_2).numberOfPatients(20L).build());
    }

    @Test
    public void testGetQuery_nullOnNotFound() throws JsonProcessingException {
        var queryFromDb = queryHandlerService.getQuery(1L);

        assertThat(queryFromDb).isNull();
    }

    @Test
    public void testGetQuery_succeedsWithNoSavedQuery() throws JsonProcessingException {
        var queryContentString = jsonUtil.writeValueAsString(createValidStructuredQuery());
        var queryContentHash = queryHashCalculator.calculateSerializedQueryBodyHash(queryContentString);
        var queryContent = new QueryContent(queryContentString);
        queryContent.setHash(queryContentHash);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(queryContent);
        var queryId = queryRepository.save(query).getId();

        var queryFromDb = queryHandlerService.getQuery(queryId);

        assertThat(queryFromDb.label()).isNull();
        assertThat(queryFromDb.comment()).isNull();
        assertThat(queryFromDb.content().inclusionCriteria()).isEqualTo(createValidStructuredQuery().inclusionCriteria());
    }

    @Test
    public void testGetQuery_succeedsWithSavedQuery() throws JsonProcessingException {
        var queryContentString = jsonUtil.writeValueAsString(createValidStructuredQuery());
        var queryContentHash = queryHashCalculator.calculateSerializedQueryBodyHash(queryContentString);
        var queryContent = new QueryContent(queryContentString);
        queryContent.setHash(queryContentHash);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(queryContent);
        var queryId = queryRepository.save(query).getId();
        var savedQuery = new SavedQuery(LABEL, COMMENT, 150L);
        queryHandlerService.saveQuery(queryId, CREATOR, savedQuery);

        var queryFromDb = queryHandlerService.getQuery(queryId);

        assertThat(queryFromDb.label()).isEqualTo(LABEL);
        assertThat(queryFromDb.comment()).isEqualTo(COMMENT);
        assertThat(queryFromDb.content().inclusionCriteria()).isEqualTo(createValidStructuredQuery().inclusionCriteria());
    }

    @Test
    public void testGetQueryContent_succeeds() throws JsonProcessingException {
        var queryContentString = jsonUtil.writeValueAsString(createValidStructuredQuery());
        var queryContentHash = queryHashCalculator.calculateSerializedQueryBodyHash(queryContentString);
        var queryContent = new QueryContent(queryContentString);
        queryContent.setHash(queryContentHash);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(queryContent);
        var queryId = queryRepository.save(query).getId();

        var queryContentFromDb = queryHandlerService.getQueryContent(queryId);

        assertThat(queryContentFromDb.inclusionCriteria()).isEqualTo(createValidStructuredQuery().inclusionCriteria());
    }

    @Test
    public void testGetQueryContent_nullIfNotFound() throws JsonProcessingException {
        var queryContentString = jsonUtil.writeValueAsString(createValidStructuredQuery());
        var queryContentHash = queryHashCalculator.calculateSerializedQueryBodyHash(queryContentString);
        var queryContent = new QueryContent(queryContentString);
        queryContent.setHash(queryContentHash);
        var query = new Query();
        query.setCreatedBy(CREATOR);
        query.setQueryContent(queryContent);
        var queryId = queryRepository.save(query).getId();

        var queryContentFromDb = queryHandlerService.getQueryContent(++queryId);

        assertThat(queryContentFromDb).isNull();
    }

    @Test
    public void testGetAuthorId_UnknownQueryIdThrows() {
        assertThrows(QueryNotFoundException.class,
            () -> queryHandlerService.getAuthorId(UNKNOWN_QUERY_ID));
    }

    @Test
    @DisplayName("saveQuery() -> duplicate labels for the same user id fails")
    public void saveQuery_DuplicateSavedQueryLabelsFails() throws Exception {
        var query1 = new Query();
        query1.setCreatedBy(CREATOR);
        var query2 = new Query();
        query2.setCreatedBy(CREATOR);
        var queryId1 = queryRepository.save(query1).getId();
        var queryId2 = queryRepository.save(query2).getId();
        var label = "label-152431";

        var savedQuery1 = new SavedQuery(label, "comment-152508", 100L);
        var savedQuery2 = new SavedQuery(label, "comment-152546", 200L);

        assertThat(queryHandlerService.saveQuery(queryId1, CREATOR, savedQuery1)).isNotNull();
        assertThrows(DataIntegrityViolationException.class,
                () -> queryHandlerService.saveQuery(queryId2, CREATOR, savedQuery2));
    }

    @Test
    @DisplayName("saveQuery() -> different labels for the same user id succeeds")
    public void saveQuery_DifferentSavedQueryLabelsSucceeds() throws Exception {
        var query1 = new Query();
        query1.setCreatedBy(CREATOR);
        var query2 = new Query();
        query2.setCreatedBy(CREATOR);
        var queryId1 = queryRepository.save(query1).getId();
        var queryId2 = queryRepository.save(query2).getId();
        var label1 = "label-152431";
        var label2 = "label-160123";

        var savedQuery1 = new SavedQuery(label1, "comment-152508", 100L);
        var savedQuery2 = new SavedQuery(label2, "comment-152546", 200L);

        assertThat(queryHandlerService.saveQuery(queryId1, CREATOR, savedQuery1)).isNotNull();
        assertDoesNotThrow(() -> queryHandlerService.saveQuery(queryId2, CREATOR, savedQuery2));
    }

    @Test
    @DisplayName("saveQuery() -> same labels for different user id succeeds")
    public void saveQuery_SameSavedQueryLabelsForDifferentUsersSucceeds() throws Exception {
        var otherCreator = "some-other-creator";
        var query1 = new Query();
        query1.setCreatedBy(CREATOR);
        var query2 = new Query();
        query2.setCreatedBy(otherCreator);
        var queryId1 = queryRepository.save(query1).getId();
        var queryId2 = queryRepository.save(query2).getId();
        var label = "label-152431";

        var savedQuery1 = new SavedQuery(label, "comment-152508", 100L);
        var savedQuery2 = new SavedQuery(label, "comment-152546", 200L);

        assertThat(queryHandlerService.saveQuery(queryId1, CREATOR, savedQuery1)).isNotNull();
        assertDoesNotThrow(() -> queryHandlerService.saveQuery(queryId2, otherCreator, savedQuery2));
    }

    @Test
    public void testDeleteSavedQuery_succeeds() throws QueryNotFoundException {
        var query1 = new Query();
        query1.setCreatedBy(CREATOR);
        var query2 = new Query();
        query2.setCreatedBy(CREATOR);
        var queryId1 = queryRepository.save(query1).getId();
        var queryId2 = queryRepository.save(query2).getId();
        var label1 = "label-152431";
        var label2 = "label-152432";

        var savedQuery1 = new SavedQuery(label1, "comment-152508", 100L);
        var savedQuery2 = new SavedQuery(label2, "comment-152546", 200L);

        queryHandlerService.saveQuery(queryId1, CREATOR, savedQuery1);
        queryHandlerService.saveQuery(queryId2, CREATOR, savedQuery2);

        assertThat(queryHandlerService.getAmountOfSavedQueriesByUser(CREATOR)).isEqualTo(2);

        queryHandlerService.deleteSavedQuery(queryId1);
        assertThat(queryHandlerService.getAmountOfSavedQueriesByUser(CREATOR)).isEqualTo(1);
        queryHandlerService.deleteSavedQuery(queryId2);
        assertThat(queryHandlerService.getAmountOfSavedQueriesByUser(CREATOR)).isEqualTo(0);
    }

    @Test
    public void testDeleteSavedQuery_failsOnUnknownQueryId() throws QueryNotFoundException {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        var label = "label-152431";

        var savedQuery = new SavedQuery(label, "comment-152508", 100L);

        queryHandlerService.saveQuery(queryId, CREATOR, savedQuery);

        assertThrows(QueryNotFoundException.class, () -> queryHandlerService.deleteSavedQuery(queryId + 1));
    }

    @Test
    public void testGetAmountOfQueriesByUserAndInterval() throws JsonProcessingException {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        queryRepository.save(query).getId();

        var count0 = queryHandlerService.getAmountOfQueriesByUserAndInterval(CREATOR, 0);
        var count1 = queryHandlerService.getAmountOfQueriesByUserAndInterval(CREATOR, 1);

        assertThat(count0).isEqualTo(0);
        assertThat(count1).isEqualTo(1);
    }

    @Test
    @DisplayName("storeQueryTemplate() -> duplicate labels for the same user id fails")
    public void storeQueryTemplate_DuplicateSavedQueryLabelsFails() throws Exception {
        var queryTemplate = QueryTemplate.builder()
                .label(LABEL)
                .comment(COMMENT)
                .createdBy(CREATOR)
                .lastModified(TIME_STRING)
                .content(createValidStructuredQuery())
                .build();

        assertDoesNotThrow(() -> queryHandlerService.storeQueryTemplate(queryTemplate, CREATOR));
        assertThrows(DataIntegrityViolationException.class,
                () -> queryHandlerService.storeQueryTemplate(queryTemplate, CREATOR));
    }

    @Test
    @DisplayName("storeQueryTemplate() -> different labels for the same user id succeeds")
    public void storeQueryTemplate_DifferentSavedQueryLabelsSucceeds() throws Exception {
        var queryTemplate1 = QueryTemplate.builder()
                .label(LABEL)
                .comment(COMMENT)
                .createdBy(CREATOR)
                .lastModified(TIME_STRING)
                .content(createValidStructuredQuery())
                .build();

        var queryTemplate2 = QueryTemplate.builder()
                .label(LABEL + "modified")
                .comment(COMMENT)
                .createdBy(CREATOR)
                .lastModified(TIME_STRING)
                .content(createValidStructuredQuery())
                .build();

        assertDoesNotThrow(() -> queryHandlerService.storeQueryTemplate(queryTemplate1, CREATOR));
        assertDoesNotThrow(() -> queryHandlerService.storeQueryTemplate(queryTemplate2, CREATOR));
    }

    @Test
    @DisplayName("storeQueryTemplate() -> same labels for different user id succeeds")
    public void storeQueryTemplate_SameSavedQueryLabelsForDifferentUsersSucceeds() throws Exception {
        var queryTemplate = QueryTemplate.builder()
                .label(LABEL)
                .comment(COMMENT)
                .createdBy(CREATOR)
                .lastModified(TIME_STRING)
                .content(createValidStructuredQuery())
                .build();

        assertDoesNotThrow(() -> queryHandlerService.storeQueryTemplate(queryTemplate, CREATOR));
        assertDoesNotThrow(() -> queryHandlerService.storeQueryTemplate(queryTemplate, "some-other-creator"));
    }

    @Test
    @DisplayName("getRetryAfterTime() -> return 0 on empty")
    public void getRetryAfterTime_zeroOnEmpty() {
        Long retryAfterTime = queryHandlerService.getRetryAfterTime(CREATOR, 0, 1000000L);
        assertThat(retryAfterTime).isEqualTo(0L);
    }

    @Test
    @DisplayName("getRetryAfterTime() -> return >0 on non empty")
    public void getRetryAfterTime_nonZeroOnNotEmpty() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        queryRepository.save(query);
        Long retryAfterTime = queryHandlerService.getRetryAfterTime(CREATOR, 0, 1000000L);
        assertThat(retryAfterTime).isGreaterThan(0L);
    }

    @Test
    @DisplayName("getAmountOfSavedQueriesByUser() -> return list size")
    public void getAmountOfSavedQueriesByUser_listSizeWhenNotEmpty() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        var label = "label-152431";
        var savedQuery = new SavedQuery(label, "comment-152508", 100L);
        queryHandlerService.saveQuery(queryId, CREATOR, savedQuery);

        var queryAmount = queryHandlerService.getAmountOfSavedQueriesByUser(CREATOR);

        assertEquals(queryAmount, 1);
    }

    @Test
    @DisplayName("getAmountOfSavedQueriesByUser() -> return 0 on empty")
    public void getAmountOfSavedQueriesByUser_zeroOnEmpty() {
        var queryAmount = queryHandlerService.getAmountOfSavedQueriesByUser(CREATOR);

        assertEquals(queryAmount, 0L);
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
