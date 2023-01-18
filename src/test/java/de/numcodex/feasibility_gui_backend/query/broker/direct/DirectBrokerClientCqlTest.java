package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class DirectBrokerClientCqlTest {

    private static final Long TEST_BACKEND_QUERY_ID = 1L;

    @SuppressWarnings("unused")
    @Mock
    FhirContext fhirContext;

    @Mock
    IGenericClient fhirClient;

    @InjectMocks
    DirectBrokerClientCql client;

    @Test
    void testPublishNonExistingQuery() {
        assertThrows(QueryNotFoundException.class, () -> client.publishQuery("does-not-exist"));
    }

    @Test
    void testPublishExistingQueryWithoutStructuredQueryDefinition() {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        assertThrows(IllegalStateException.class, () -> client.publishQuery(queryId));
    }

    @Test
    void testGetSiteName() {
        assertEquals("CQL Server", client.getSiteName("1"));
        assertTrue(client.getSiteName("foo").isEmpty());
        assertTrue(client.getSiteName("something-else").isEmpty());
        assertTrue(client.getSiteName("FHIR Server").isEmpty());
    }

    @Test
    void testCloseQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.closeQuery("does-not-exist"));
    }

    @Test
    void testCloseQuery() {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        assertDoesNotThrow(() -> client.closeQuery(queryId));
    }

    @Test
    void testGetResultFeasibilityForQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.getResultFeasibility("does-not-exist", "unknown-site-id"));
    }

    @Test
    void testGetResultFeasibilityForUnknownSite() {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        assertThrows(SiteNotFoundException.class, () -> client.getResultFeasibility(queryId, "unknown-site-id"));
    }

    @Test
    void testGetResultSiteIdsForQueryWhichDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> client.getResultSiteIds("does-not-exist"));
    }

    @Test
    void testGetResultSiteIdsForUnpublishedQuery() throws QueryNotFoundException {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var resultSiteIds = client.getResultSiteIds(queryId);

        assertTrue(resultSiteIds.isEmpty());
    }
}
