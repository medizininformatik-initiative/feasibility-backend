package de.numcodex.feasibility_gui_backend.query.translation;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.cql.Library;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Tag("query")
@Tag("translation")
@ExtendWith(MockitoExtension.class)
public class CqlQueryTranslatorTest {
    @Mock
    private Translator translator;

    @Spy
    private ObjectMapper jsonUtil;

    @InjectMocks
    private CqlQueryTranslator cqlQueryTranslator;

    @Test
    public void testTranslate_ModelConversionFailsDuringEncoding() throws JsonProcessingException {
        var testQuery = new StructuredQuery(null, null, null, null);
        doThrow(JsonProcessingException.class).when(jsonUtil).writeValueAsString(testQuery);

        assertThrows(QueryTranslationException.class, () -> cqlQueryTranslator.translate(testQuery));
        verify(jsonUtil, never()).readValue(anyString(), eq(de.numcodex.sq2cql.model.structured_query.StructuredQuery.class));
        verifyNoInteractions(translator);
    }

    @Test
    public void testTranslate_ModelConversionFailsDuringDecoding() throws JsonProcessingException {
        var testQuery = new StructuredQuery(null, null, null, null);
        doReturn("foo").when(jsonUtil).writeValueAsString(testQuery);
        doThrow(JsonProcessingException.class).when(jsonUtil).readValue("foo",
                de.numcodex.sq2cql.model.structured_query.StructuredQuery.class);

        assertThrows(QueryTranslationException.class, () -> cqlQueryTranslator.translate(testQuery));
        verify(jsonUtil).readValue("foo", de.numcodex.sq2cql.model.structured_query.StructuredQuery.class);
        verifyNoInteractions(translator);
    }

    @Disabled("Needs to be enabled if the new version of sq2cl is available and compatible with structured query v2.")
    @Test
    public void testTranslate_TranslationFails() {
        var termCode = new TermCode("LL2191-6", "http://loinc.org", null, "Geschlecht");
        var inclusionCriterion = new Criterion(List.of(termCode), null, null, null);
        var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"), List.of(List.of(inclusionCriterion)), List.of(List.of()), "foo");

        doThrow(NullPointerException.class).when(translator)
                .toCql(any(de.numcodex.sq2cql.model.structured_query.StructuredQuery.class));

        assertThrows(QueryTranslationException.class, () -> cqlQueryTranslator.translate(testQuery));
        verify(translator).toCql(any());
    }

    @Disabled("Needs to be enabled if the new version of sq2cl is available and compatible with structured query v2.")
    @Test
    public void testTranslate_EverythingSucceeds() throws QueryTranslationException {
        var termCode = new TermCode("LL2191-6", "http://loinc.org", null, "Geschlecht");
        var inclusionCriterion = new Criterion(List.of(termCode), null, null, null);
        var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"), List.of(List.of(inclusionCriterion)), List.of(List.of()), "foo");

        var resultLibraryMock = mock(Library.class);
        when(resultLibraryMock.print()).thenReturn("bar");
        when(translator.toCql(any(de.numcodex.sq2cql.model.structured_query.StructuredQuery.class)))
                .thenReturn(resultLibraryMock);

        var translationResult = cqlQueryTranslator.translate(testQuery);
        assertEquals("bar", translationResult);

    }
}
