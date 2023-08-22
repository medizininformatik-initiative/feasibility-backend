package de.numcodex.feasibility_gui_backend.terminology;

import de.numcodex.feasibility_gui_backend.terminology.api.CategoryEntry;
import de.numcodex.feasibility_gui_backend.terminology.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;

@Tag("terminology")
@ExtendWith(MockitoExtension.class)
class TerminologyServiceTest {

    private static UUID CATEGORY_1_ID = UUID.fromString("2ec77ac6-2547-2aff-031b-337d9ff80cff");
    private static UUID CATEGORY_2_ID = UUID.fromString("457b3f3b-bf4e-45da-b676-dc63d31942dd");
    private static UUID VALID_NODE_ID_CAT1 = UUID.fromString("72ceaea9-c1ff-2e94-5fc0-7ba34feca654");
    private static UUID VALID_NODE_ID_CAT2 = UUID.fromString("33ca0320-81e5-406f-bdfd-c649e443ddd6");

    private static UUID INVALID_NODE_ID = UUID.fromString("00000000-1111-2222-3333-444444444444");

    @Mock
    private UiProfileRepository uiProfileRepository;

    @Mock
    private TermCodeRepository termCodeRepository;

    @Mock
    private ContextualizedTermCodeRepository contextualizedTermCodeRepository;

    @Mock
    private MappingRepository mappingRepository;

    private TerminologyService createTerminologyService(String uiProfilePath) throws IOException {
        return new TerminologyService(uiProfilePath, uiProfileRepository, termCodeRepository, contextualizedTermCodeRepository, mappingRepository);
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(uiProfileRepository, termCodeRepository, contextualizedTermCodeRepository, mappingRepository);
    }

    @Test
    void testInitFailsOnNonexistingFolderWithNullpointerException() {
        assertThrows(NullPointerException.class, () -> createTerminologyService("does/not/exist"));
    }

    @Test
    void testInitFailsOnBogusEntriesWithIoException() {
        assertThrows(IOException.class, () -> createTerminologyService("src/test/resources/ontology/ui_profiles_bogus"));
    }

    @Test
    void testGetEntry_succeeds() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        var getEntryResult = assertDoesNotThrow(() -> terminologyService.getEntry(VALID_NODE_ID_CAT1));

        assertThat(getEntryResult.getId()).isEqualTo(VALID_NODE_ID_CAT1);
    }

    @Test
    void testGetEntry_throwsOnUnknown() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        assertThrows(NodeNotFoundException.class, () -> terminologyService.getEntry(INVALID_NODE_ID));
    }

    @Test
    void testGetCategories() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        var categoriesResult = assertDoesNotThrow(() -> terminologyService.getCategories());
        assertNotNull(categoriesResult);
        assertFalse(categoriesResult.isEmpty());
        assertThat(categoriesResult)
                .hasSize(2)
                .extracting(CategoryEntry::getDisplay, CategoryEntry::getCatId)
                .containsExactlyInAnyOrder(
                        tuple("Category1", CATEGORY_1_ID),
                        tuple("Category2", CATEGORY_2_ID)
                );
    }

    @Test
    void testGetSelectableEntries() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        var selectableEntriesResult_cat1_total = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc1_", CATEGORY_1_ID));
        var selectableEntriesResult_cat1_part = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc1_1", CATEGORY_1_ID));
        var selectableEntriesResult_cat2_total = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc2_", CATEGORY_2_ID));
        var selectableEntriesResult_cat2_empty = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc3_", CATEGORY_2_ID));

        // Cat1 contains the codes tc1_1 up to tc1_16, Cat 2 contains tc2_1 and tc2_2
        assertEquals(16, selectableEntriesResult_cat1_total.size());
        assertEquals(8, selectableEntriesResult_cat1_part.size());
        assertEquals(2, selectableEntriesResult_cat2_total.size());
        assertTrue(selectableEntriesResult_cat2_empty.isEmpty());
    }

    @Test
    void testGetSelectableEntriesWithoutCategory() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        var selectableEntriesResult_cat1_total = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc1_", null));
        var selectableEntriesResult_cat1_part = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc1_1", null));
        var selectableEntriesResult_cat2_total = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc2_", null));
        var selectableEntriesResult_cat2_empty = assertDoesNotThrow(() -> terminologyService.getSelectableEntries("tc3_", null));

        // Cat1 contains the codes tc1_1 up to tc1_16, Cat 2 contains tc2_1 and tc2_2
        assertEquals(16, selectableEntriesResult_cat1_total.size());
        assertEquals(8, selectableEntriesResult_cat1_part.size());
        assertEquals(2, selectableEntriesResult_cat2_total.size());
        assertTrue(selectableEntriesResult_cat2_empty.isEmpty());
    }

    @Test
    void getUiProfile_succeedsOnKnownHash() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        doReturn(Optional.of(createUiProfile())).when(uiProfileRepository).findByContextualizedTermcodeHash(any(String.class));

        var uiProfileResult = assertDoesNotThrow(() -> terminologyService.getUiProfile(VALID_NODE_ID_CAT1.toString()));
        assertFalse(uiProfileResult.isBlank());
    }

    @Test
    void getUiProfile_throwsOnUnknownHash() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        doReturn(Optional.empty()).when(uiProfileRepository).findByContextualizedTermcodeHash(any(String.class));

        assertThrows(UiProfileNotFoundException.class, () -> terminologyService.getUiProfile(INVALID_NODE_ID.toString()));
    }

    @Test
    void getMapping_succeedsOnKnownHash() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        doReturn(Optional.of(createMapping())).when(mappingRepository).findByContextualizedTermcodeHash(any(String.class));

        var mappingResult = assertDoesNotThrow(() -> terminologyService.getMapping(VALID_NODE_ID_CAT1.toString()));
        assertFalse(mappingResult.isBlank());
    }

    @Test
    void getMapping_throwsOnUnknownHash() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        doReturn(Optional.empty()).when(mappingRepository).findByContextualizedTermcodeHash(any(String.class));

        assertThrows(MappingNotFoundException.class, () -> terminologyService.getMapping(INVALID_NODE_ID.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void isExistingTermCode(boolean doesExist) throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        doReturn(doesExist).when(termCodeRepository).existsTermCode(any(String.class), any(String.class));
        doReturn(doesExist).when(termCodeRepository).existsTermCode(any(String.class), any(String.class), any(String.class));

        boolean termCodeResultWithVersion = terminologyService.isExistingTermCode("some-system", "some-code", "some-version");
        boolean termCodeResultWithoutVersion = terminologyService.isExistingTermCode("some-system", "some-code", null);

        assertEquals(termCodeResultWithVersion, doesExist);
        assertEquals(termCodeResultWithoutVersion, doesExist);
    }

    @Test
    void testMin() {
        int expected = 3;
        int[] numbers = {33, 18, 3, 30, 4};

        int result = TerminologyService.min(numbers);

        assertEquals(expected, result);
    }

    @Test
    void testMin_emptyArray() {
        int expected = Integer.MAX_VALUE;
        int[] numbers = {};

        int result = TerminologyService.min(numbers);

        assertEquals(expected, result);
    }

    @Test
    void getIntersection_succeeds() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        doReturn(List.of()).when(contextualizedTermCodeRepository).filterByCriteriaSetUrl(any(String.class), anyList());

        var intersection = terminologyService.getIntersection("http://foo.bar", List.of(UUID.randomUUID().toString()));

        assertTrue(intersection.isEmpty());
    }

    @Test
    void getIntersection_succeedsWithEmptyList() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        doReturn(List.of()).when(contextualizedTermCodeRepository).filterByCriteriaSetUrl(any(String.class), anyList());

        var intersection = terminologyService.getIntersection("http://foo.bar", List.of());

        assertTrue(intersection.isEmpty());
    }

    private UiProfile createUiProfile() {
        var uiProfile = new UiProfile();
        uiProfile.setId(1);
        uiProfile.setName("example");
        uiProfile.setUiProfile("""
                {
                	"name": "ExampleProfile",
                	"time_restriction_allowed": true
                }
                """);
        return uiProfile;
    }

    private Mapping createMapping() {
        var mapping = new Mapping();
        mapping.setId(1);
        mapping.setName("example");
        mapping.setType("mapping-type");
        mapping.setContent("""
                {
                	"name": "ExampleMapping",
                	"content": "tbd"
                }
                """
        );

        return mapping;
    }
}
