package de.numcodex.feasibility_gui_backend.service.query_executor.impl.dsf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DSFQueryManagerTest {

    private static final String ORGANIZATION = "My-ZARS";

    @Captor
    ArgumentCaptor<Bundle> bundleCaptor;

    @Mock
    private FhirWebserviceClient fhirWebserviceClient;

    @Mock
    private FhirWebClientProvider fhirWebClientProvider;

    private DSFQueryManager queryHandler;
    private String unknownQueryId;

    @BeforeEach
    public void setUp() {
        this.queryHandler = new DSFQueryManager(fhirWebClientProvider, ORGANIZATION);
        this.unknownQueryId = UUID.randomUUID().toString();
    }

    @Test
    public void testCreateQuery_QueryIdsDiffer() {
        String queryA = queryHandler.createQuery();
        String queryB = queryHandler.createQuery();

        assertNotEquals(queryA, queryB);
    }

    @Test
    public void testAddQueryDefinition_QueryNotFound() {
        assertThrows(QueryNotFoundException.class, () -> queryHandler.addQueryDefinition(unknownQueryId, "text/cql", ""));
    }

    @Test
    public void testAddQueryDefinition_NoCQLMediaType() {
        String queryId = queryHandler.createQuery();
        assertThrows(UnsupportedMediaTypeException.class, () -> queryHandler.addQueryDefinition(queryId, "application/json", ""));
    }

    @Test
    public void testAddQueryDefinition_DefinitionAlreadyPresent() throws UnsupportedMediaTypeException, QueryNotFoundException {
        String queryId = queryHandler.createQuery();
        queryHandler.addQueryDefinition(queryId, "text/cql", "");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> queryHandler.addQueryDefinition(queryId, "text/cql", ""));
        assertEquals("Query with ID '" + queryId + "' already contains a query definition.", e.getMessage());
    }

    @Test
    public void testPublishQuery_QueryNotFound() {
        assertThrows(QueryNotFoundException.class, () -> queryHandler.publishQuery(unknownQueryId));
    }


    @Test
    public void testPublishQuery_QueryHasNoQueryDefinitionYet() {
        String queryId = queryHandler.createQuery();

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> queryHandler.publishQuery(queryId));
        assertEquals("Query with ID '" + queryId + "' does not contain query definition yet.", e.getMessage());
    }

    @Test
    public void testPublishQuery_PublishFailed() throws UnsupportedMediaTypeException, QueryNotFoundException {
        String queryId = queryHandler.createQuery();
        queryHandler.addQueryDefinition(queryId, "text/cql", "");

        when(fhirWebserviceClient.postBundle(any(Bundle.class))).thenThrow();

        assertThrows(IOException.class, () -> queryHandler.publishQuery(queryId));
    }

    @Test
    public void testPublishQuery() throws UnsupportedMediaTypeException, QueryNotFoundException, IOException, FhirWebClientProvisionException {
        String queryId = queryHandler.createQuery();
        queryHandler.addQueryDefinition(queryId, "text/cql", "");

        when(fhirWebClientProvider.provideFhirWebserviceClient()).thenReturn(fhirWebserviceClient);
        queryHandler.publishQuery(queryId);

        verify(fhirWebserviceClient).postBundle(bundleCaptor.capture());


        List<BundleEntryComponent> bundleEntries = bundleCaptor.getValue().getEntry();
        Task task = (Task) bundleEntries.stream().filter(e -> e.getResource().fhirType().equals("Task")).findFirst()
                .orElseThrow().getResource();

        String businessKey = task.getInput().stream().filter(i -> i.getType().getCodingFirstRep().getCode().equals("business-key"))
                .findFirst().orElseThrow().getValue().toString();

        assertEquals(businessKey, queryId);
    }

    @Test
    public void testRemoveQuery_QueryNotFound() {
        assertThrows(QueryNotFoundException.class, () -> queryHandler.removeQuery(unknownQueryId));
    }

    @Test
    public void testRemoveQuery() throws QueryNotFoundException {
        String queryId = queryHandler.createQuery();

        queryHandler.removeQuery(queryId);
        assertThrows(QueryNotFoundException.class, () -> queryHandler.removeQuery(queryId));
    }
}
