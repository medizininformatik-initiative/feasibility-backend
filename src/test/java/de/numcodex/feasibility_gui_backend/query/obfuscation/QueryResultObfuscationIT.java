package de.numcodex.feasibility_gui_backend.query.obfuscation;


import de.numcodex.feasibility_gui_backend.query.persistence.*;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.query.result.ResultService;
import de.numcodex.feasibility_gui_backend.query.result.ResultServiceSpringConfig;
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
        QueryObfuscationSpringConfig.class,
        ResultServiceSpringConfig.class
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class QueryResultObfuscationIT {

    @Autowired
    private QueryContentRepository queryContentRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private ResultService resultService;

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
        testQueryA.setCreatedBy("someone");
        queryRepository.save(testQueryA);

        var testQueryB = new Query();
        testQueryB.setQueryContent(testQueryContent);
        testQueryB.setCreatedAt(Timestamp.from(Instant.now()));
        testQueryB.setCreatedBy("someone");
        queryRepository.save(testQueryB);

        var testSite = "A";

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult1 = new ResultLine(testSite, SUCCESS, 10L);
        resultService.addResultLine(testQueryA.getId(), testSiteAResult1);

        var testSiteAResult2 = new ResultLine(testSite, SUCCESS, 20L);
        resultService.addResultLine(testQueryB.getId(), testSiteAResult2);

        var tokenTestSiteAResult1 = queryResultObfuscator.tokenizeSiteName(testQueryA.getId(), testSite);
        var tokenTestSiteAResult2 = queryResultObfuscator.tokenizeSiteName(testQueryB.getId(), testSite);

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
        testQueryA.setCreatedBy("someone");
        queryRepository.save(testQueryA);

        var testSite = "A";

        // Dispatch entries are left out for brevity. Also, they do not matter for this test scenario.

        var testSiteAResult = new ResultLine(testSite, SUCCESS, 10L);
        resultService.addResultLine(testQueryA.getId(), testSiteAResult);

        var tokenTestSiteAResult1 = queryResultObfuscator.tokenizeSiteName(testQueryA.getId(), testSite);
        var tokenTestSiteAResult2 = queryResultObfuscator.tokenizeSiteName(testQueryA.getId(), testSite);

        assertEquals(tokenTestSiteAResult1, tokenTestSiteAResult2);
    }
}
