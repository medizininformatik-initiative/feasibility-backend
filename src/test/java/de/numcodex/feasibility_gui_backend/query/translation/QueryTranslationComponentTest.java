package de.numcodex.feasibility_gui_backend.query.translation;

import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.CQL;
import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.STRUCTURED_QUERY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("query")
@Tag("translation")
@ExtendWith(MockitoExtension.class)
public class QueryTranslationComponentTest {

    @Mock
    private QueryTranslator firstQueryTranslator;

    @Mock
    private QueryTranslator secondQueryTranslator;

    private static StructuredQuery testQuery;

    @BeforeAll
    public static void setUp() {
        testQuery = new StructuredQuery(null, null, null, null);
    }

    @AfterEach
    public void resetMocks() {
        clearInvocations(firstQueryTranslator, secondQueryTranslator);
    }

    private QueryTranslationComponent setUpComponent(Map<QueryMediaType, QueryTranslator> translators) {
        return new QueryTranslationComponent(translators);
    }

    @Test
    public void testTranslate_NoTranslatorsYieldEmptyResultMap() {
        var queryTranslationComponent = setUpComponent(Map.of());
        var translationResult = assertDoesNotThrow(() -> queryTranslationComponent.translate(testQuery));
        assertTrue(translationResult.isEmpty());
    }

    @Test
    public void testTranslate_TranslationStopsOnFirstException() throws QueryTranslationException {
        var translators = new LinkedHashMap<QueryMediaType, QueryTranslator>();
        translators.put(STRUCTURED_QUERY, firstQueryTranslator);
        translators.put(CQL, secondQueryTranslator);
        var queryTranslationComponent = setUpComponent(translators);
        doThrow(QueryTranslationException.class).when(firstQueryTranslator).translate(testQuery);

        assertThrows(QueryTranslationException.class, () -> queryTranslationComponent.translate(testQuery));
        verify(firstQueryTranslator).translate(testQuery);
        verifyNoInteractions(secondQueryTranslator);
    }

    @Test
    public void testTranslate_MultipleTranslatorsYieldMultipleTranslationResults() throws QueryTranslationException {
        var queryTranslationComponent = setUpComponent(Map.of(
                STRUCTURED_QUERY, firstQueryTranslator,
                CQL, secondQueryTranslator
        ));
        doReturn("foo").when(firstQueryTranslator).translate(testQuery);
        doReturn("bar").when(secondQueryTranslator).translate(testQuery);

        var translationsResults = assertDoesNotThrow(() -> queryTranslationComponent.translate(testQuery));
        verify(firstQueryTranslator).translate(testQuery);
        verify(secondQueryTranslator).translate(testQuery);

        assertEquals(2, translationsResults.size());
        assertTrue(translationsResults.containsKey(STRUCTURED_QUERY));
        assertEquals("foo", translationsResults.get(STRUCTURED_QUERY));
        assertTrue(translationsResults.containsKey(CQL));
        assertEquals("bar", translationsResults.get(CQL));
    }
}
