package de.numcodex.feasibility_gui_backend.query.obfuscation;


import de.numcodex.feasibility_gui_backend.query.persistence.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;

import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag("query")
@Tag("obfuscation")
@Import({
        QueryObfuscationSpringConfig.class
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
public class QueryResultObfuscationIT {

    @Autowired
    private QueryContentRepository queryContentRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QueryResultObfuscator queryResultObfuscator;

    @Test
    public void testTokenizeSiteName_ResultsOfTheSameSiteProduceDifferentTokensForDifferentQueries() {
        var testQueryContent = new QueryContent("irrelevant-for-this-test");
        testQueryContent.setHash("ab34ffcd"); // irrelevant for this test, too
        queryContentRepository.save(testQueryContent);

        var testQueryA = new Query();
        testQueryA.setQueryContent(testQueryContent);
        testQueryA.setCreatedAt(Timestamp.from(Instant.now()));
        queryRepository.save(testQueryA);

        var testQueryB = new Query();
        testQueryB.setQueryContent(testQueryContent);
        testQueryB.setCreatedAt(Timestamp.from(Instant.now()));
        queryRepository.save(testQueryB);

        var testSite = new Site();
        testSite.setSiteName("A");
        siteRepository.save(testSite);

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult1 = new Result();
        testSiteAResult1.setSite(testSite);
        testSiteAResult1.setQuery(testQueryA);
        testSiteAResult1.setResult(10);
        testSiteAResult1.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteAResult1.setResultType(SUCCESS);
        resultRepository.save(testSiteAResult1);

        var testSiteAResult2 = new Result();
        testSiteAResult2.setSite(testSite);
        testSiteAResult2.setQuery(testQueryB);
        testSiteAResult2.setResult(20);
        testSiteAResult2.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteAResult2.setResultType(SUCCESS);
        resultRepository.save(testSiteAResult2);

        var tokenTestSiteAResult1 = queryResultObfuscator.tokenizeSiteName(testSiteAResult1);
        var tokenTestSiteAResult2 = queryResultObfuscator.tokenizeSiteName(testSiteAResult2);

        assertNotEquals(tokenTestSiteAResult1, tokenTestSiteAResult2);
    }

    // This one is very important since the UI is actually polling. Thus, the results need to be stable!
    @Test
    public void testTokenizeSiteName_MultipleCallsProduceTheSameToken() {
        var testQueryContent = new QueryContent("irrelevant-for-this-test");
        testQueryContent.setHash("ab34ffcd"); // irrelevant for this test, too
        queryContentRepository.save(testQueryContent);

        var testQueryA = new Query();
        testQueryA.setQueryContent(testQueryContent);
        testQueryA.setCreatedAt(Timestamp.from(Instant.now()));
        queryRepository.save(testQueryA);

        var testSite = new Site();
        testSite.setSiteName("A");
        siteRepository.save(testSite);

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult = new Result();
        testSiteAResult.setSite(testSite);
        testSiteAResult.setQuery(testQueryA);
        testSiteAResult.setResult(10);
        testSiteAResult.setReceivedAt(Timestamp.from(Instant.now()));
        testSiteAResult.setResultType(SUCCESS);
        resultRepository.save(testSiteAResult);

        var tokenTestSiteAResult1 = queryResultObfuscator.tokenizeSiteName(testSiteAResult);
        var tokenTestSiteAResult2 = queryResultObfuscator.tokenizeSiteName(testSiteAResult);

        assertEquals(tokenTestSiteAResult1, tokenTestSiteAResult2);
    }
}
