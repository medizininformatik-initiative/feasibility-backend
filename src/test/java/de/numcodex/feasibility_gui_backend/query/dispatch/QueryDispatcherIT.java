package de.numcodex.feasibility_gui_backend.query.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.db.QueryContent;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.translation.QueryTranslatorSpringConfig;
import de.numcodex.feasibility_gui_backend.repository.QueryContentRepository;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@Tag("query")
@Tag("dispatch")
@Import({
        QueryTranslatorSpringConfig.class,
        QueryDispatchSpringConfig.class
})
@DataJpaTest(
        properties = {
                "app.cqlTranslationEnabled=false",
                "app.fhirTranslationEnabled=false"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
public class QueryDispatcherIT {

    @Autowired
    private QueryDispatcher queryDispatcher;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private QueryContentRepository queryContentRepository;

    @Autowired
    @Qualifier("translation")
    private ObjectMapper jsonUtil;

    @Autowired
    private QueryHashCalculator queryHashCalculator;

    @Test
    public void testEnqueueNewQuery_QueryContentGetsCreatedIfNotAlreadyPresent() throws JsonProcessingException {
        var otherQuery = new StructuredQuery();
        otherQuery.setVersion(URI.create("https://to_be_decided.com/draft-2/schema#"));
        var serializedOtherQuery = jsonUtil.writeValueAsString(otherQuery);
        var serializedOtherQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedOtherQuery);

        var otherQueryContent = new QueryContent(serializedOtherQuery);
        otherQueryContent.setHash(serializedOtherQueryHash);
        queryContentRepository.save(otherQueryContent);


        var testQuery = new StructuredQuery();
        var serializedTestQuery = jsonUtil.writeValueAsString(testQuery);
        var serializedTestQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedTestQuery);

        assertDoesNotThrow(() -> queryDispatcher.enqueueNewQuery(testQuery));

        var queryContent = queryContentRepository.findByHash(serializedTestQueryHash);
        assertEquals(2, queryContentRepository.count());
        assertTrue(queryContent.isPresent());
        assertEquals(serializedTestQuery, queryContent.get().getQueryContent());
    }

    @Test
    public void testEnqueueNewQuery_QueryContentGetsReusedIfAlreadyPresent() throws JsonProcessingException {
        var testQuery = new StructuredQuery();
        var serializedTestQuery = jsonUtil.writeValueAsString(testQuery);
        var serializedTestQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedTestQuery);

        var queryContent = new QueryContent(serializedTestQuery);
        queryContent.setHash(serializedTestQueryHash);
        queryContentRepository.save(queryContent);

        assertDoesNotThrow(() -> queryDispatcher.enqueueNewQuery(testQuery));
        assertEquals(1, queryContentRepository.count());
    }

    @Test
    public void testEnqueueNewQuery() throws JsonProcessingException {
        var testQuery = new StructuredQuery();
        var serializedTestQuery = jsonUtil.writeValueAsString(testQuery);
        var serializedTestQueryHash = queryHashCalculator.calculateSerializedQueryBodyHash(serializedTestQuery);

        assertDoesNotThrow(() -> queryDispatcher.enqueueNewQuery(testQuery));

        var enqueuedQueries = queryRepository.findAll();
        assertEquals(1, enqueuedQueries.size());
        assertEquals(serializedTestQuery, enqueuedQueries.get(0).getQueryContent().getQueryContent());
        assertEquals(serializedTestQueryHash, enqueuedQueries.get(0).getQueryContent().getHash());
        assertNotNull(enqueuedQueries.get(0).getCreatedAt());
        assertNull(enqueuedQueries.get(0).getResults());
    }
}
