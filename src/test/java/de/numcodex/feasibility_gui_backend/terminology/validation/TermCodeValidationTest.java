package de.numcodex.feasibility_gui_backend.terminology.validation;

import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
class TermCodeValidationTest {

    @Mock
    private TerminologyService terminologyService;

    private TermCodeValidation termCodeValidation;

    @BeforeEach
    void setUp() {
        termCodeValidation = new TermCodeValidation(terminologyService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void getInvalidTermCodes_emptyOnValidTermcodes(boolean withExclusionCriteria) {
        doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());

        var invalidTermCodes = termCodeValidation.getInvalidTermCodes(createValidStructuredQuery(withExclusionCriteria));

        assertTrue(invalidTermCodes.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void getInvalidTermCodes_notEmptyOnInvalidTermcode(boolean withExclusionCriteria) {
        doReturn(false).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());

        var invalidTermCodes = termCodeValidation.getInvalidTermCodes(createValidStructuredQuery(withExclusionCriteria));

        assertFalse(invalidTermCodes.isEmpty());
        assertEquals(withExclusionCriteria ? 2 : 1, invalidTermCodes.size());
    }

    @NotNull
    private static StructuredQuery createValidStructuredQuery(boolean withExclusionCriteria) {
        var termCode = TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();
        var criterion = Criterion.builder()
                .termCodes(List.of(termCode))
                .attributeFilters(List.of())
                .build();
        return StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(criterion)))
                .exclusionCriteria(withExclusionCriteria ? List.of(List.of(criterion)) : List.of())
                .display("foo")
                .build();
    }
}
