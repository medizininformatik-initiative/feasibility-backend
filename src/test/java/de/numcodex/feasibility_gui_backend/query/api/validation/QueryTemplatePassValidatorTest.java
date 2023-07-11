package de.numcodex.feasibility_gui_backend.query.api.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import java.net.URI;
import java.util.List;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("query")
@Tag("api")
@Tag("validation-template")
@ExtendWith(MockitoExtension.class)
public class QueryTemplatePassValidatorTest {

    @Spy
    private QueryTemplatePassValidator validator;

    @Mock
    private ConstraintValidatorContext ctx;

    @Test
    public void testIsValid_validQueryPasses() {
        var termCode = new TermCode("LL2191-6", "http://loinc.org", null, "Geschlecht");

        var inclusionCriterion = new Criterion(List.of(termCode), null, null, null);

        var testQuery = new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"), List.of(List.of(inclusionCriterion)), List.of(), "foo");

        var testStoredQuery = new QueryTemplate(0, testQuery, "test", null, null, null, null, null);

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testStoredQuery, ctx));
        assertTrue(validationResult);
    }

    @Test
    public void testIsValid_invalidQueryPasses() {
        var testStoredQuery = new QueryTemplate(0, null, null, null, null, null, null, null);

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testStoredQuery, ctx));
        assertTrue(validationResult);
    }
}
