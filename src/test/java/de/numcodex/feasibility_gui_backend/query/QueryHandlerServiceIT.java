package de.numcodex.feasibility_gui_backend.query;

import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultLine;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerSpringConfig;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryObfuscationSpringConfig;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.result.ResultServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateHandler;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.DETAILED;
import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.DETAILED_OBFUSCATED;
import static de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail.SUMMARY;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("query")
@Tag("handler")
@Import({
        BrokerSpringConfig.class,
        QueryTranslatorSpringConfig.class,
        QueryDispatchSpringConfig.class,
        QueryCollectSpringConfig.class,
        QueryHandlerService.class,
        QueryObfuscationSpringConfig.class,
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

    @Autowired
    private QueryHandlerService queryHandlerService;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private QueryDispatchRepository queryDispatchRepository;

    @Autowired
    private ResultService resultService;

    @Test
    public void testRunQuery() {
        var testStructuredQuery = new StructuredQuery();

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

        assertThat(queryResult.getQueryId()).isEqualTo(UNKNOWN_QUERY_ID);
        assertThat(queryResult.getTotalNumberOfPatients()).isZero();
        assertThat(queryResult.getResultLines()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource
    public void testGetQueryResult_ErrorResultsAreIgnored(ResultDetail resultDetail) {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(), new ResultLine(SITE_NAME_1, ERROR, 0L));

        var queryResult = queryHandlerService.getQueryResult(queryId, resultDetail);

        assertThat(queryResult.getResultLines()).isEmpty();
    }

    @Test
    public void testGetQueryResult_SummaryContainsOnlyTheTotal() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(), new ResultLine(SITE_NAME_1, SUCCESS, 10L));
        resultService.addResultLine(query.getId(), new ResultLine(SITE_NAME_2, SUCCESS, 20L));

        var queryResult = queryHandlerService.getQueryResult(queryId, SUMMARY);

        assertThat(queryResult.getTotalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.getResultLines()).isEmpty();
    }

    @Test
    public void testGetQueryResult_DetailedObfuscatedDoesNotContainTheSiteNames() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(), new ResultLine(SITE_NAME_1, SUCCESS, 10L));
        resultService.addResultLine(query.getId(), new ResultLine(SITE_NAME_2, SUCCESS, 20L));

        var queryResult = queryHandlerService.getQueryResult(queryId, DETAILED_OBFUSCATED);

        assertThat(queryResult.getTotalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.getResultLines()).hasSize(2);
        assertThat(queryResult.getResultLines().stream().map(QueryResultLine::getSiteName))
            .doesNotContain(SITE_NAME_1, SITE_NAME_2);
        assertThat(queryResult.getResultLines().stream().map(QueryResultLine::getNumberOfPatients))
            .contains(10L, 20L);
    }

    @Test
    public void testGetQueryResult_DetailedContainsTheSiteNames() {
        var query = new Query();
        query.setCreatedBy(CREATOR);
        var queryId = queryRepository.save(query).getId();
        resultService.addResultLine(query.getId(), new ResultLine(SITE_NAME_1, SUCCESS, 10L));
        resultService.addResultLine(query.getId(), new ResultLine(SITE_NAME_2, SUCCESS, 20L));

        var queryResult = queryHandlerService.getQueryResult(queryId, DETAILED);

        assertThat(queryResult.getTotalNumberOfPatients()).isEqualTo(30L);
        assertThat(queryResult.getResultLines())
            .hasSize(2)
            .contains(QueryResultLine.builder().siteName(SITE_NAME_1).numberOfPatients(10L).build(),
                QueryResultLine.builder().siteName(SITE_NAME_2).numberOfPatients(20L).build());
    }
}
