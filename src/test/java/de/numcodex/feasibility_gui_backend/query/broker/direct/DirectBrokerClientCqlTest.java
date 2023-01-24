package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.broker.QueryDefinitionNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import java.io.IOException;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectBrokerClientCqlTest {

    private static final Long TEST_BACKEND_QUERY_ID = 1L;

    @SuppressWarnings("unused")
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    FhirConnector fhirConnector;

    @InjectMocks
    DirectBrokerClientCql client;

    @Test
    void testPublishNonExistingQuery() {
        assertThrows(QueryNotFoundException.class, () -> client.publishQuery("does-not-exist"));
    }

    @Test
    void testPublishExistingQueryWithoutStructuredQueryDefinition() {
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        assertThrows(QueryDefinitionNotFoundException.class, () -> client.publishQuery(queryId));
    }

    @Test
    void testPublishExistingQueryWithIOExceptionInCreateBundle() throws IOException {
        when(fhirConnector.createBundle(any(String.class), any(String.class), any(String.class))).thenThrow(IOException.class);
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var cqlString = FhirConnector.getResourceFileAsString("gender-male.cql");
        assertDoesNotThrow(() -> client.addQueryDefinition(queryId, QueryMediaType.CQL, cqlString));
        assertThrows(IOException.class, () -> client.publishQuery(queryId));
    }

    @Test
    void testPublishExistingQueryWithIOExceptionInTransmitBundle() throws IOException {
        doThrow(IOException.class).when(fhirConnector).transmitBundle(any(Bundle.class));
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var cqlString = FhirConnector.getResourceFileAsString("gender-male.cql");
        assertDoesNotThrow(() -> client.addQueryDefinition(queryId, QueryMediaType.CQL, cqlString));
        assertThrows(IOException.class, () -> client.publishQuery(queryId));
    }

    @Test
    void testPublishExistingQueryWithIOExceptionInEvaluateMeasure() throws IOException {
        when(fhirConnector.evaluateMeasure(any(String.class))).thenThrow(IOException.class);
        var queryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var cqlString = FhirConnector.getResourceFileAsString("gender-male.cql");
        assertDoesNotThrow(() -> client.addQueryDefinition(queryId, QueryMediaType.CQL, cqlString));
        assertThrows(IOException.class, () -> client.publishQuery(queryId));
    }

    @Test
    void testGetSiteName() {
        assertDoesNotThrow(() -> assertEquals("Local Server", client.getSiteName("1")));
        assertThrows(SiteNotFoundException.class, () -> client.getSiteName("foo"));
        assertThrows(SiteNotFoundException.class, () -> client.getSiteName("something-else"));
        assertThrows(SiteNotFoundException.class, () -> client.getSiteName("FHIR Server"));
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
