package QueryExecutor.impl.dsf;

import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DSFQueryResultStoreTest {

    private QueryResultStore store;

    @BeforeEach
    public void setUp() {
        store = new DSFQueryResultStore();
    }

    @Test
    public void testStoreInitialResultForQuery() throws QueryNotFoundException, ClientNotFoundException {
        store.storeResult(new DSFQueryResult("foo", "bar", 1));
        assertEquals(1, store.getMeasureCount("foo", "bar"));
    }

    @Test
    public void testStoreFurtherResultsForQuery() throws QueryNotFoundException, ClientNotFoundException {
        store.storeResult(new DSFQueryResult("foo", "bar", 1));
        store.storeResult(new DSFQueryResult("foo", "baz", 11));
        assertEquals(1, store.getMeasureCount("foo", "bar"));
        assertEquals(11, store.getMeasureCount("foo", "baz"));
    }

    @Test
    public void testGetMeasureCountButQueryDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> store.getMeasureCount("not-existing-query", "bar"));
    }

    @Test
    public void testGetMeasureCountButQueryHasNoClientResultYet() {
        store.storeResult(new DSFQueryResult("foo", "bar", 1));
        assertThrows(ClientNotFoundException.class, () -> store.getMeasureCount("foo", "not-existing-client"));
    }

    @Test
    public void testGetMeasureCount() throws QueryNotFoundException, ClientNotFoundException {
        store.storeResult(new DSFQueryResult("foo", "bar", 1));
        assertEquals(1, store.getMeasureCount("foo", "bar"));
    }

    @Test
    public void testGetClientIdsWithResultButQueryDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> store.getClientIdsWithResult("not-existing-query"));
    }

    @Test
    public void testGetClientIdsWithResult() throws QueryNotFoundException {
        store.storeResult(new DSFQueryResult("foo", "bar", 1));
        store.storeResult(new DSFQueryResult("foo", "baz", 11));

        List<String> expectedClientIds = List.of("bar", "baz");
        List<String> actualClientIds = store.getClientIdsWithResult("foo");

        assertEquals(expectedClientIds, actualClientIds);
    }

    @Test
    public void testRemoveResultButQueryDoesNotExist() {
        assertThrows(QueryNotFoundException.class, () -> store.removeResult("not-existing-query"));
    }

    @Test
    public void testRemoveResult() throws QueryNotFoundException {
        store.storeResult(new DSFQueryResult("foo", "bar", 1));
        store.removeResult("foo");

        assertThrows(QueryNotFoundException.class, () -> store.getClientIdsWithResult("foo"));
    }
}
