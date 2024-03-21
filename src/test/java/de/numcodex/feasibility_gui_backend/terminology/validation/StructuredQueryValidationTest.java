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
class StructuredQueryValidationTest {

    @Mock
    private TerminologyService terminologyService;

    private StructuredQueryValidation structuredQueryValidation;

    @BeforeEach
    void setUp() {
        structuredQueryValidation = new StructuredQueryValidation(terminologyService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void getInvalidCriteria_emptyOnValidCriteria(boolean withExclusionCriteria) {
        doReturn(true).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());

        var invalidCriteria = structuredQueryValidation.getInvalidCriteria(createValidStructuredQuery(withExclusionCriteria));

        assertTrue(invalidCriteria.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void getInvalidCriteria_notEmptyOnInvalidTermcode(boolean withExclusionCriteria) {
        doReturn(false).when(terminologyService).isExistingTermCode(any(String.class), any(String.class), isNull());

        var invalidCriteria = structuredQueryValidation.getInvalidCriteria(createValidStructuredQuery(withExclusionCriteria));

        assertFalse(invalidCriteria.isEmpty());
        assertEquals(withExclusionCriteria ? 2 : 1, invalidCriteria.size());
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
}
