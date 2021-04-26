package de.numcodex.feasibility_gui_backend.service.query_executor.impl.dsf;

import de.numcodex.feasibility_gui_backend.service.QueryMediaTypes;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DSFMediaTypeTranslatorTest {

    private DSFMediaTypeTranslator translator;

    @BeforeEach
    public void setUp() {
        translator = new DSFMediaTypeTranslator();
    }

    @Test
    public void testTranslate_StructuredQueryMediaType() throws UnsupportedMediaTypeException {
        var dsfMediaType = translator.translate(QueryMediaTypes.STRUCTURED_QUERY);
        assertEquals("application/json", dsfMediaType);
    }

    @Test
    public void testTranslate_CqlQueryMediaType() throws UnsupportedMediaTypeException {
        var dsfMediaType = translator.translate(QueryMediaTypes.CQL);
        assertEquals(QueryMediaTypes.CQL, dsfMediaType);
    }

    @Test
    public void testTranslate_FhirQueryMediaType() throws UnsupportedMediaTypeException {
        var dsfMediaType = translator.translate(QueryMediaTypes.FHIR);
        assertEquals("application/x-fhir-query", dsfMediaType);
    }

    @Test
    public void testTranslate_UnsupportedMediaType() {
        assertThrows(UnsupportedMediaTypeException.class, () -> translator.translate("something/unsupported"));
    }
}
