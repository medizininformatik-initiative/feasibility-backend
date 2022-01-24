package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import de.numcodex.feasibility_gui_backend.query.broker.UnsupportedMediaTypeException;
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
        var dsfMediaType = translator.translate("application/sq+json");
        assertEquals("application/json", dsfMediaType);
    }

    @Test
    public void testTranslate_CqlQueryMediaType() throws UnsupportedMediaTypeException {
        var dsfMediaType = translator.translate("text/cql");
        assertEquals("text/cql", dsfMediaType);
    }

    @Test
    public void testTranslate_FhirQueryMediaType() throws UnsupportedMediaTypeException {
        var dsfMediaType = translator.translate("text/fhir-codex");
        assertEquals("application/x-fhir-query", dsfMediaType);
    }

    @Test
    public void testTranslate_UnsupportedMediaType() {
        assertThrows(UnsupportedMediaTypeException.class, () -> translator.translate("something/unsupported"));
    }
}
