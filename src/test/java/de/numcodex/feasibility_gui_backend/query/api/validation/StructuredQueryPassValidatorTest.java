package de.numcodex.feasibility_gui_backend.query.api.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("query")
@Tag("api")
@Tag("validation")
@ExtendWith(MockitoExtension.class)
public class StructuredQueryPassValidatorTest {

    @Spy
    private StructuredQueryPassValidator validator;

    @Mock
    private ConstraintValidatorContext ctx;

    @Test
    public void testIsValid_validQueryPasses() {
        var termCode = new TermCode("LL2191-6", "http://loinc.org", null, "Geschlecht");
        var inclusionCriterion = new Criterion(List.of(termCode), null, null, null);
        var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"), List.of(List.of(inclusionCriterion)), List.of(), "foo");

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testQuery, ctx));
        assertTrue(validationResult);
    }

    @Test
    public void testIsValid_invalidQueryPasses() {
        var testQuery = new StructuredQuery(null, null, null, null);
        var validationResult = assertDoesNotThrow(() -> validator.isValid(testQuery, ctx));
        assertTrue(validationResult);
    }
}
