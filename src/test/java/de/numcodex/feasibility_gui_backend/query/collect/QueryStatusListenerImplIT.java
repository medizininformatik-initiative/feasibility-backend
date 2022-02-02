package de.numcodex.feasibility_gui_backend.query.collect;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.FAILED;
import static de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType.DIRECT;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

// TODO: maybe we should check the whole setup at this point - dispatch, process, collect? -> discuss in review
@Tag("query")
@Tag("collect")
@Import(QueryCollectSpringConfig.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
class QueryStatusListenerImplIT {

    @Autowired
    private QueryContentRepository queryContentRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QueryStatusListener queryStatusListener;

    private static final String TEST_SITE_NAME = "TestSite";
    private static final int TEST_MATCHES_IN_POPULATION = 100;
    private static final String BROKER_QUERY_ID = "37ff21b1-3d5f-49b0-ac09-063c94eac7aa";
    private static final BrokerClientType FAKE_BROKER_CLIENT_TYPE = DIRECT;

    private String testSiteId;
    private Long testBackendQueryId;

    @BeforeEach
    public void setUpDatabaseState() {
        var fakeContent = new QueryContent("{}");
        fakeContent.setHash("a2189dffb");
        queryContentRepository.save(fakeContent);

        var testQuery = new Query();
        testQuery.setQueryContent(fakeContent);
        testBackendQueryId = queryRepository.save(testQuery).getId();

        var testSite = new Site();
        testSite.setSiteName(TEST_SITE_NAME);
        testSiteId = String.valueOf(siteRepository.save(testSite).getId());

        // Dispatch Table setup has been left out for brevity (will be filled at runtime but is not important for any of
        // the test cases).
    }

    @ParameterizedTest
    @EnumSource(value = QueryStatus.class,
            names = {"COMPLETED", "FAILED"},
            mode = EnumSource.Mode.EXCLUDE)
    public void testPersistResult_NonTerminatingStatusChangesDoesNotLeadToPersistedResult(QueryStatus status) {
        var fakeBrokerClient = new FakeBrokerClient();

        var statusUpdate = new QueryStatusUpdate(fakeBrokerClient, BROKER_QUERY_ID, testSiteId, status);
        assertDoesNotThrow(() -> queryStatusListener.onClientUpdate(testBackendQueryId, statusUpdate));
        assertEquals(0, resultRepository.count());
    }

    @Test
    public void testPersistResult_UnknownBrokerQueryIdDoesNotLeadToPersistedResult() {
        var fakeBrokerClient = new FakeBrokerClient();
        var unknownBrokerQueryId = "some_unknown_id";

        var statusUpdate = new QueryStatusUpdate(fakeBrokerClient, unknownBrokerQueryId, testSiteId, COMPLETED);
        assertDoesNotThrow(() -> queryStatusListener.onClientUpdate(testBackendQueryId, statusUpdate));
        assertEquals(0, resultRepository.count());
    }

    @Test
    public void testPersistResult_CompleteStatusLeadsToPersistedResultWithMatchesInPopulation() {
        var fakeBrokerClient = new FakeBrokerClient();

        var statusUpdate = new QueryStatusUpdate(fakeBrokerClient, BROKER_QUERY_ID, testSiteId, COMPLETED);
        assertDoesNotThrow(() -> queryStatusListener.onClientUpdate(testBackendQueryId, statusUpdate));

        var registeredResults = resultRepository.findAll();
        assertEquals(1, registeredResults.size());
        assertEquals(SUCCESS, registeredResults.get(0).getResultType());
        assertEquals(TEST_SITE_NAME, registeredResults.get(0).getSite().getSiteName());
        assertEquals(TEST_MATCHES_IN_POPULATION, registeredResults.get(0).getResult());
    }

    @Test
    public void testPersistResult_FailedStatusLeadsToPersistedResultWithoutMatchesInPopulation() {
        var fakeBrokerClient = new FakeBrokerClient();

        var statusUpdate = new QueryStatusUpdate(fakeBrokerClient, BROKER_QUERY_ID, testSiteId, FAILED);
        assertDoesNotThrow(() -> queryStatusListener.onClientUpdate(testBackendQueryId, statusUpdate));

        var registeredResults = resultRepository.findAll();
        assertEquals(1, registeredResults.size());
        assertEquals(ERROR, registeredResults.get(0).getResultType());
        assertEquals(TEST_SITE_NAME, registeredResults.get(0).getSite().getSiteName());
        assertNull(registeredResults.get(0).getResult());
    }

    private static class FakeBrokerClient implements BrokerClient {

        @Override
        public BrokerClientType getBrokerType() {
            return FAKE_BROKER_CLIENT_TYPE;
        }

        @Override
        public void addQueryStatusListener(QueryStatusListener queryStatusListener) {
            // NO-OP
        }

        @Override
        public String createQuery(Long backendQueryId) {
            // NO-OP
            return null;
        }

        @Override
        public void addQueryDefinition(String brokerQueryId, String mediaType, String content) {
            // NO-OP
        }

        @Override
        public void publishQuery(String brokerQueryId) {
            // NO-OP
        }

        @Override
        public void closeQuery(String brokerQueryId) {
            // NO-OP
        }

        @Override
        public int getResultFeasibility(String brokerQueryId, String siteId) throws QueryNotFoundException {
            if (!brokerQueryId.equals(BROKER_QUERY_ID)) {
                throw new QueryNotFoundException("cannot find broker specific query for id '%s'"
                        .formatted(brokerQueryId));
            }
            return TEST_MATCHES_IN_POPULATION;

        }

        @Override
        public List<String> getResultSiteIds(String brokerQueryId) {
            // NO-OP
            return null;
        }

        @Override
        public String getSiteName(String siteId) {
            return TEST_SITE_NAME;
        }
    }
}