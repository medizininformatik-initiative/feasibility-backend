package de.numcodex.feasibility_gui_backend.service;


import de.numcodex.feasibility_gui_backend.model.db.Query;
import de.numcodex.feasibility_gui_backend.model.db.QueryContent;
import de.numcodex.feasibility_gui_backend.model.db.Result;
import de.numcodex.feasibility_gui_backend.model.db.Result.ResultId;
import de.numcodex.feasibility_gui_backend.model.db.Site;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.collect.QueryCollectSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchSpringConfig;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;
import de.numcodex.feasibility_gui_backend.repository.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;

import static de.numcodex.feasibility_gui_backend.model.db.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.model.db.ResultType.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("handler")
@Import({
        ServiceSpringConfig.class,
        QueryTranslatorSpringConfig.class,
        QueryDispatchSpringConfig.class,
        QueryCollectSpringConfig.class,
        QueryHandlerService.class
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
        var testQueryId = queryContentRepository.save(testQueryContent).getId();

        var testQuery = new Query();
        testQuery.setId(testQueryId);
        testQuery.setQueryContent(testQueryContent);
        testQuery.setCreatedAt(Timestamp.from(Instant.now()));
        queryRepository.save(testQuery);

        var testSiteA = new Site();
        testSiteA.setSiteName("A");
        var testSiteAId = siteRepository.save(testSiteA).getId();

        var testSiteB = new Site();
        testSiteB.setSiteName("B");
        var testSiteBId = siteRepository.save(testSiteB).getId();

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResultId = new ResultId();
        testSiteAResultId.setSiteId(testSiteAId);
        testSiteAResultId.setQueryId(testQueryId);

        var testSiteAResult = new Result();
        testSiteAResult.setId(testSiteAResultId);
        testSiteAResult.setSite(testSiteA);
        testSiteAResult.setQuery(testQuery);
        testSiteAResult.setResult(10);
        testSiteAResult.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteAResult.setResultType(SUCCESS);
        resultRepository.save(testSiteAResult);

        var testSiteBResultId = new ResultId();
        testSiteBResultId.setSiteId(testSiteBId);
        testSiteBResultId.setQueryId(testQueryId);

        var testSiteBResult = new Result();
        testSiteBResult.setId(testSiteBResultId);
        testSiteBResult.setSite(testSiteB);
        testSiteBResult.setQuery(testQuery);
        testSiteBResult.setResult(20);
        testSiteBResult.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteBResult.setResultType(SUCCESS);
        resultRepository.save(testSiteBResult);

        var queryResult = assertDoesNotThrow(() -> queryHandlerService.getQueryResult(testQueryId));

        var siteAResultLine = queryResult.getResultLines().stream().filter(l -> l.getSiteName()
                .equals("A")).findFirst().orElseThrow();
        var siteBResultLine = queryResult.getResultLines().stream().filter(l -> l.getSiteName()
                .equals("B")).findFirst().orElseThrow();

        assertEquals(30, queryResult.getTotalNumberOfPatients());
        assertEquals(10, siteAResultLine.getNumberOfPatients());
        assertEquals(20, siteBResultLine.getNumberOfPatients());
    }
}
