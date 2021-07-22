package de.numcodex.feasibility_gui_backend.service.query_executor.impl.mock;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.SiteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockBrokerClientTest {

    MockBrokerClient client;

    @BeforeEach
    void setUp() {
        client = new MockBrokerClient();
    }

    @Test
    void testAddQueryDefinitionToNonExistingQuery() {
        assertDoesNotThrow(() -> client.addQueryDefinition("does-not-exist", "application/json", ""));
    }

    @Test
    void testAddQueryDefinitionToExistingQuery() {
        var queryId = client.createQuery();
        assertDoesNotThrow(() -> client.addQueryDefinition(queryId, "application/json", ""));
    }

    @Test
    void testCloseQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.closeQuery("does-not-exist"));
    }

    @Test
    void testCloseQueryWhichHasNotYetBeenPublished() {
        var queryId = client.createQuery();
        assertDoesNotThrow(() -> client.closeQuery(queryId));
    }

    @Test
    void testGetResultFeasibilityForQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.getResultFeasibility("does-not-exist", "foo"));
    }

    @Test
    void testGetResultFeasibilityForUnknownSite() {
        var queryId = client.createQuery();
        assertThrows(SiteNotFoundException.class, () -> client.getResultFeasibility(queryId, "unknown-site-id"));
    }

    @Test
    void testGetResultSiteIdsForQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.getResultSiteIds("does-not-exist"));
    }

    @Test
    void testGetResultSiteIdsForUnpublishedQuery() throws QueryNotFoundException {
        var queryId = client.createQuery();
        var resultSiteIds = client.getResultSiteIds(queryId);

        assertTrue(resultSiteIds.isEmpty());
    }

    @Test
    void testGetSiteName() {
        assertEquals("Lübeck", client.getSiteName("1"));
        assertEquals("Erlangen", client.getSiteName("2"));
        assertEquals("Frankfurt", client.getSiteName("3"));
        assertEquals("Leipzig", client.getSiteName("4"));
        assertEquals("", client.getSiteName("5"));
        assertEquals("", client.getSiteName("foo"));
        assertEquals("", client.getSiteName("anything-other-than-1-to-4"));
    }
}
