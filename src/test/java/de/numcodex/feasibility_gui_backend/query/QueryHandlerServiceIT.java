package de.numcodex.feasibility_gui_backend.query;


import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerSpringConfig;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryObfuscationSpringConfig;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryResultObfuscator;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
        QueryObfuscationSpringConfig.class
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
@SuppressWarnings("NewClassNamingConvention")
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
    private SiteRepository siteRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QueryResultObfuscator queryResultObfuscator;

    @Test
    public void testRunQuery() {
        var testStructuredQuery = new StructuredQuery();

        assertDoesNotThrow(() -> queryHandlerService.runQuery(testStructuredQuery));
        assertEquals(1, queryRepository.count());
        assertEquals(1, queryDispatchRepository.count());
    }

    // This behavior seems to be necessary since the UI is polling constantly.
    // If the response was an error then the UI would need to handle it accordingly.
    // TODO: We should discuss this with the UI team. Maybe a better solution can be identified.
    @Test
    public void testGetQueryResult_UnknownQueryIdLeadsToResultWithZeroMatchesInPopulation() {
        var unknownQueryId = 9999999L;

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(unknownQueryId));
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
        var testQueryId = queryRepository.save(testQuery).getId();

        var testSiteA = new Site();
        testSiteA.setSiteName("A");
        siteRepository.save(testSiteA);

        var testSiteB = new Site();
        testSiteB.setSiteName("B");
        siteRepository.save(testSiteB);

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult = new Result();
        testSiteAResult.setSite(testSiteA);
        testSiteAResult.setQuery(testQuery);
        testSiteAResult.setResult(10);
        testSiteAResult.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteAResult.setResultType(SUCCESS);
        resultRepository.save(testSiteAResult);

        var testSiteBResult = new Result();
        testSiteBResult.setSite(testSiteB);
        testSiteBResult.setQuery(testQuery);
        testSiteBResult.setResult(20);
        testSiteBResult.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteBResult.setResultType(SUCCESS);
        resultRepository.save(testSiteBResult);

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(testQueryId));

        var testSiteAResultTokenizedSiteName = queryResultObfuscator.tokenizeSiteName(testSiteAResult);
        var siteAResultLine = queryResult.getResultLines().stream().filter(l -> l.getSiteName()
                .equals(testSiteAResultTokenizedSiteName)).findFirst().orElseThrow();

        var testSiteBResultTokenizedSiteName = queryResultObfuscator.tokenizeSiteName(testSiteBResult);
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
        var testQueryId = queryRepository.save(testQuery).getId();

        var testSiteA = new Site();
        testSiteA.setSiteName("A");
        siteRepository.save(testSiteA);

        var testSiteB = new Site();
        testSiteB.setSiteName("B");
        siteRepository.save(testSiteB);

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult = new Result();
        testSiteAResult.setSite(testSiteA);
        testSiteAResult.setQuery(testQuery);
        testSiteAResult.setResult(10);
        testSiteAResult.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteAResult.setResultType(SUCCESS);
        resultRepository.save(testSiteAResult);

        var testSiteBResult = new Result();
        testSiteBResult.setSite(testSiteB);
        testSiteBResult.setQuery(testQuery);
        testSiteBResult.setResult(20);
        testSiteBResult.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteBResult.setResultType(ERROR);
        resultRepository.save(testSiteBResult);

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(testQueryId));

        assertEquals(10, queryResult.getTotalNumberOfPatients());
        assertEquals(1, queryResult.getResultLines().size());
    }
}
