package de.numcodex.feasibility_gui_backend.query;


import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerSpringConfig;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryObfuscationSpringConfig;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryResultObfuscator;
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

import java.sql.Timestamp;
import java.time.Instant;

import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

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

    @Autowired
    private QueryHandlerService queryHandlerService;

    @Autowired
    private QueryContentRepository queryContentRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private QueryDispatchRepository queryDispatchRepository;

    @Autowired
    private ResultService resultService;

    @Autowired
    private QueryResultObfuscator queryResultObfuscator;

    @Test
    public void testRunQuery() {
        var testStructuredQuery = new StructuredQuery();

        queryHandlerService.runQuery(testStructuredQuery, "test").block();
        assertEquals(1, queryRepository.count());
        assertEquals(1, queryDispatchRepository.count());
    }

    // This behavior seems to be necessary since the UI is polling constantly.
    // If the response was an error then the UI would need to handle it accordingly.
    // TODO: We should discuss this with the UI team. Maybe a better solution can be identified.
    @Test
    public void testGetQueryResult_UnknownQueryIdLeadsToResultWithZeroMatchesInPopulation() {
        var unknownQueryId = 9999999L;

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(unknownQueryId, ResultDetail.DETAILED_OBFUSCATED));
        assertNotNull(queryResult);
        assertEquals(unknownQueryId, queryResult.getQueryId());
        assertEquals(0, queryResult.getTotalNumberOfPatients());
        assertTrue(queryResult.getResultLines().isEmpty());
    }

    @Test
    public void testGetQueryResult_MatchesInPopulationGetAccumulated() {
        var testQueryContent = new QueryContent("irrelevant-for-this-test");
        testQueryContent.setHash("ab34ffcd"); // irrelevant for this test, too
        queryContentRepository.save(testQueryContent);

        var testQuery = new Query();
        testQuery.setQueryContent(testQueryContent);
        testQuery.setCreatedAt(Timestamp.from(Instant.now()));
        testQuery.setCreatedBy("someone");
        var testQueryId = queryRepository.save(testQuery).getId();

        var testSiteA = "A";
        var testSiteB = "B";

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult = new ResultLine(testSiteA, SUCCESS, 10L);
        resultService.addResultLine(testQuery.getId(), testSiteAResult);

        var testSiteBResult = new ResultLine(testSiteB, SUCCESS, 20L);
        resultService.addResultLine(testQuery.getId(), testSiteBResult);

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(testQueryId, ResultDetail.DETAILED_OBFUSCATED));

        var testSiteAResultTokenizedSiteName = queryResultObfuscator.tokenizeSiteName(testQuery.getId(), testSiteA);
        var siteAResultLine = queryResult.getResultLines().stream().filter(l -> l.getSiteName()
                .equals(testSiteAResultTokenizedSiteName)).findFirst().orElseThrow();

        var testSiteBResultTokenizedSiteName = queryResultObfuscator.tokenizeSiteName(testQuery.getId(), testSiteB);
        var siteBResultLine = queryResult.getResultLines().stream().filter(l -> l.getSiteName()
                .equals(testSiteBResultTokenizedSiteName)).findFirst().orElseThrow();

        assertEquals(30, queryResult.getTotalNumberOfPatients());
        assertEquals(10, siteAResultLine.getNumberOfPatients());
        assertEquals(20, siteBResultLine.getNumberOfPatients());
    }

    @Test
    public void testGetQueryResult_FailedResultsDoNotAffectResult() {
        var testQueryContent = new QueryContent("irrelevant-for-this-test");
        testQueryContent.setHash("ab34ffcd"); // irrelevant for this test, too
        queryContentRepository.save(testQueryContent);

        var testQuery = new Query();
        testQuery.setQueryContent(testQueryContent);
        testQuery.setCreatedAt(Timestamp.from(Instant.now()));
        testQuery.setCreatedBy("someone");
        var testQueryId = queryRepository.save(testQuery).getId();

        var testSiteA = "A";
        var testSiteB = "B";

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult = new ResultLine(testSiteA, SUCCESS, 10L);
        resultService.addResultLine(testQuery.getId(), testSiteAResult);

        var testSiteBResult = new ResultLine(testSiteB, ERROR, 20L);
        resultService.addResultLine(testQuery.getId(), testSiteBResult);

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(testQueryId, ResultDetail.DETAILED_OBFUSCATED));

        assertEquals(10, queryResult.getTotalNumberOfPatients());
        assertEquals(1, queryResult.getResultLines().size());
    }

    @ParameterizedTest
    @EnumSource(ResultDetail.class)
    public void testGetQueryResult_getCorrectResultDetail(ResultDetail resultDetail) {
        var testQueryContent = new QueryContent("irrelevant-for-this-test");
        testQueryContent.setHash("ab34ffcd"); // irrelevant for this test, too
        queryContentRepository.save(testQueryContent);

        var testQuery = new Query();
        testQuery.setQueryContent(testQueryContent);
        testQuery.setCreatedAt(Timestamp.from(Instant.now()));
        testQuery.setCreatedBy("someone");
        var testQueryId = queryRepository.save(testQuery).getId();

        var testSiteA = "A";
        var testSiteB = "B";

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult = new ResultLine(testSiteA, SUCCESS, 10L);
        resultService.addResultLine(testQuery.getId(), testSiteAResult);

        var testSiteBResult = new ResultLine(testSiteB, SUCCESS, 20L);
        resultService.addResultLine(testQuery.getId(), testSiteBResult);

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(testQueryId, resultDetail));

        switch(resultDetail) {
            case SUMMARY -> {
                assertEquals(30, queryResult.getTotalNumberOfPatients());
                assertEquals(0, queryResult.getResultLines().size());
            }
            case DETAILED_OBFUSCATED -> {
                assertEquals(30, queryResult.getTotalNumberOfPatients());
                assertEquals(2, queryResult.getResultLines().size());
                assertEquals(0, queryResult.getResultLines().stream().filter(rl -> testSiteA.equals(rl.getSiteName())).toList().size());
                assertEquals(0, queryResult.getResultLines().stream().filter(rl -> testSiteB.equals(rl.getSiteName())).toList().size());
            }
            case DETAILED -> {
                assertEquals(30, queryResult.getTotalNumberOfPatients());
                assertEquals(2, queryResult.getResultLines().size());
                assertEquals(1, queryResult.getResultLines().stream().filter(rl -> testSiteA.equals(rl.getSiteName())).toList().size());
                assertEquals(1, queryResult.getResultLines().stream().filter(rl -> testSiteB.equals(rl.getSiteName())).toList().size());
            }
        }

    }
}
