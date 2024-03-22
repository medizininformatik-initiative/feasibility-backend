package de.numcodex.feasibility_gui_backend.terminology.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("terminology")
@ExtendWith(MockitoExtension.class)
class StructuredQueryValidationTest {

    @Mock
    private TerminologyService terminologyService;

    private StructuredQueryValidation structuredQueryValidation;

    @BeforeEach
    void setUp() {
        structuredQueryValidation = new StructuredQueryValidation(terminologyService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIsValid_trueOnValidCriteria(boolean withExclusionCriteria) {
        doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());

        var isValid = structuredQueryValidation.isValid(createValidStructuredQuery(withExclusionCriteria));

        assertTrue(isValid);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIsValid_falseOnInvalidCriteria(boolean withExclusionCriteria) {
        doReturn(false).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());

        var isValid = structuredQueryValidation.isValid(createValidStructuredQuery(withExclusionCriteria));

        assertFalse(isValid);
    }

    @Test
    void testIsValid_falseOnMissingContext() {
        var isValid = structuredQueryValidation.isValid(createStructuredQueryWithoutContext());

        assertFalse(isValid);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void testAnnotateStructuredQuery_emptyIssuesOnValidCriteriaOrSkippedValidation(String withExclusionCriteriaString, String skipValidationString) {
        boolean withExclusionCriteria = Boolean.parseBoolean(withExclusionCriteriaString);
        boolean skipValidation = Boolean.parseBoolean(skipValidationString);
        if (!skipValidation) {
            doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());
        }

        var annotatedStructuredQuery = structuredQueryValidation.annotateStructuredQuery(createValidStructuredQuery(withExclusionCriteria), skipValidation);

        assertTrue(annotatedStructuredQuery.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void testAnnotateStructuredQuery_nonEmptyIssuesOnInvalidCriteria(String withExclusionCriteriaString, String skipValidationString) {
        boolean withExclusionCriteria = Boolean.parseBoolean(withExclusionCriteriaString);
        boolean skipValidation = Boolean.parseBoolean(skipValidationString);
        if (!skipValidation) {
            doReturn(false).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());
        }

        var annotatedStructuredQuery = structuredQueryValidation.annotateStructuredQuery(createValidStructuredQuery(withExclusionCriteria), skipValidation);

        if (skipValidation) {
            assertTrue(annotatedStructuredQuery.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
        } else {
            assertFalse(annotatedStructuredQuery.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAnnotateStructuredQuery_nonEmptyIssuesOnMissingContext(boolean skipValidation) {
        var annotatedStructuredQuery = structuredQueryValidation.annotateStructuredQuery(createStructuredQueryWithoutContext(), skipValidation);

        if (skipValidation) {
            assertTrue(annotatedStructuredQuery.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
        } else {
            assertFalse(annotatedStructuredQuery.inclusionCriteria().get(0).get(0).validationIssues().isEmpty());
        }
    }

    @NotNull
    private static StructuredQuery createValidStructuredQuery(boolean withExclusionCriteria) {
        var context = TermCode.builder()
            .code("Laboruntersuchung")
            .system("fdpg.mii.cds")
            .display("Laboruntersuchung")
            .version("1.0.0")
            .build();
        var termCode = TermCode.builder()
                .code("19113-0")
                .system("http://loinc.org")
                .display("IgE")
                .build();
        var criterion = Criterion.builder()
                .termCodes(List.of(termCode))
                .context(context)
                .attributeFilters(List.of())
                .build();
        return StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(criterion)))
                .exclusionCriteria(withExclusionCriteria ? List.of(List.of(criterion)) : List.of())
                .display("foo")
                .build();
    }

    @NotNull
    private static StructuredQuery createStructuredQueryWithoutContext() {
        var termCode = TermCode.builder()
            .code("19113-0")
            .system("http://loinc.org")
            .display("IgE")
            .build();
        var criterion = Criterion.builder()
            .termCodes(List.of(termCode))
            .attributeFilters(List.of())
            .build();
        return StructuredQuery.builder()
            .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
            .inclusionCriteria(List.of(List.of(criterion)))
            .exclusionCriteria(List.of())
            .display("foo")
            .build();
    }
}
