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
        var termCode = TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();

        var inclusionCriterion = Criterion.builder()
                .termCodes(List.of(termCode))
                .build();

        var testQuery = StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(inclusionCriterion)))
                .exclusionCriteria(List.of())
                .display("foo")
                .build();
        var testStoredQuery = QueryTemplate.builder()
                .id(0)
                .content(testQuery)
                .label("test")
                .build();

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testStoredQuery, ctx));
        assertTrue(validationResult);
    }

    @Test
    public void testIsValid_invalidQueryPasses() {
        var testStoredQuery = QueryTemplate.builder()
                .id(0)
                .build();

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testStoredQuery, ctx));
        assertTrue(validationResult);
    }
}
