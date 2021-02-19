package QueryExecutor.impl.dsf;

import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.ClientNotFoundException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DSFQueryResultCollectorTest {

    @Mock
    QueryResultStore store;

    @InjectMocks
    DSFQueryResultCollector collector;

    @Test
    public void testGetResultFeasibilityButQueryIsNotFound() throws QueryNotFoundException, ClientNotFoundException {
        when(store.getMeasureCount("foo", "bar")).thenThrow(QueryNotFoundException.class);
        assertThrows(QueryNotFoundException.class, () -> collector.getResultFeasibility("foo", "bar"));
    }

    @Test
    public void testGetResultFeasibilityButClientIsUnknown() throws QueryNotFoundException, ClientNotFoundException {
        when(store.getMeasureCount("foo", "bar")).thenThrow(ClientNotFoundException.class);
        assertThrows(ClientNotFoundException.class, () -> collector.getResultFeasibility("foo", "bar"));
    }

    @Test
    public void testGetResultFeasibility() throws QueryNotFoundException, ClientNotFoundException {
        when(store.getMeasureCount("foo", "bar")).thenReturn(1);
        int actualResultFeasibility = collector.getResultFeasibility("foo", "bar");

        assertEquals(1, actualResultFeasibility);
    }

    @Test
    public void testGetResultClientIdsButQueryIsNotFound() throws QueryNotFoundException {
        when(store.getClientIdsWithResult("foo")).thenThrow(QueryNotFoundException.class);
        assertThrows(QueryNotFoundException.class, () -> collector.getResultClientIds("foo"));
    }

    @Test
    public void testGetResultClientIds() throws QueryNotFoundException {
        List<String> expectedClientIds = Lists.newArrayList("bar", "baz");
        when(store.getClientIdsWithResult("foo")).thenReturn(expectedClientIds);

        List<String> actualClientIds = collector.getResultClientIds("foo");

        assertEquals(expectedClientIds, actualClientIds);
    }

    @Test
    public void testRemoveResultsButQueryIsNotFound() throws QueryNotFoundException {
        Mockito.doThrow(QueryNotFoundException.class).when(store).removeResult("foo");
        assertThrows(QueryNotFoundException.class, () -> collector.removeResults("foo"));
    }
}
