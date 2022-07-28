package de.numcodex.feasibility_gui_backend.query.api.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("query")
@Tag("api")
@Tag("validation-stored")
@ExtendWith(MockitoExtension.class)
public class StoredQueryPassValidatorTest {

    @Spy
    private StoredQueryPassValidator validator;

    @Mock
    private ConstraintValidatorContext ctx;

    @Test
    public void testIsValid_validQueryPasses() {
        var termCode = new TermCode();
        termCode.setCode("LL2191-6");
        termCode.setSystem("http://loinc.org");
        termCode.setDisplay("Geschlecht");

        var inclusionCriterion = new Criterion();
        inclusionCriterion.setTermCodes(new ArrayList<>(List.of(termCode)));

        var testQuery = new StructuredQuery();
        testQuery.setInclusionCriteria(List.of(List.of(inclusionCriterion)));
        testQuery.setExclusionCriteria(List.of());
        testQuery.setDisplay("foo");
        testQuery.setVersion(URI.create("http://to_be_decided.com/draft-2/schema#"));

        var testStoredQuery = new StoredQuery();
        testStoredQuery.setStructuredQuery(testQuery);
        testStoredQuery.setLabel("test");

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testStoredQuery, ctx));
        assertTrue(validationResult);
    }

    @Test
    public void testIsValid_invalidQueryPasses() {
        var testStoredQuery = new StoredQuery();

        var validationResult = assertDoesNotThrow(() -> validator.isValid(testStoredQuery, ctx));
        assertTrue(validationResult);
    }
}
