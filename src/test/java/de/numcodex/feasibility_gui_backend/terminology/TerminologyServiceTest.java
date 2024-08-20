package de.numcodex.feasibility_gui_backend.terminology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.terminology.api.CategoryEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.CriteriaProfileData;
import de.numcodex.feasibility_gui_backend.terminology.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

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
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminologyServiceTest {
    private static UUID CATEGORY_1_ID = UUID.fromString("2ec77ac6-2547-2aff-031b-337d9ff80cff");
    private static UUID CATEGORY_2_ID = UUID.fromString("457b3f3b-bf4e-45da-b676-dc63d31942dd");
    private static UUID CATEGORY_A_ID = UUID.fromString("30a20f30-77db-11ee-b962-0242ac120002");
    private static UUID CATEGORY_B_ID = UUID.fromString("385d2db8-77db-11ee-b962-0242ac120002");
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

    private final ObjectMapper jsonUtil = new ObjectMapper();

    @Mock
    private Resource terminologySystemsResource;

    private TerminologyService createTerminologyService(String uiProfilePath) throws IOException {
        return new TerminologyService(uiProfilePath,"src/test/resources/ontology/terminology_systems.json", uiProfileRepository, termCodeRepository, contextualizedTermCodeRepository, mappingRepository, jsonUtil);
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
    void testGetCategories_order1() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        ReflectionTestUtils.setField(terminologyService, "sortedCategories", List.of("Category2", "Category3", "Category1"));

        var categoriesResult = assertDoesNotThrow(terminologyService::getCategories);
        assertNotNull(categoriesResult);
        assertFalse(categoriesResult.isEmpty());
        assertThat(categoriesResult)
                .hasSize(4)
                .extracting(CategoryEntry::getDisplay, CategoryEntry::getCatId)
                .containsExactly(
                        tuple("Category2", CATEGORY_2_ID),
                        tuple("Category1", CATEGORY_1_ID),
                        tuple("CategoryA", CATEGORY_A_ID),
                        tuple("CategoryB", CATEGORY_B_ID)
                );
    }

    @Test
    void testGetCategories_order2() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        ReflectionTestUtils.setField(terminologyService, "sortedCategories", List.of("CategoryB", "CategoryX", "CategoryY"));

        var categoriesResult = assertDoesNotThrow(terminologyService::getCategories);
        assertNotNull(categoriesResult);
        assertFalse(categoriesResult.isEmpty());
        assertThat(categoriesResult)
                .hasSize(4)
                .extracting(CategoryEntry::getDisplay, CategoryEntry::getCatId)
                .containsExactly(
                        tuple("CategoryB", CATEGORY_B_ID),
                        tuple("Category1", CATEGORY_1_ID),
                        tuple("Category2", CATEGORY_2_ID),
                        tuple("CategoryA", CATEGORY_A_ID)
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

    @Test
    void getCriteriaProfileData_emptyIdsResultInEmptyList() throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");

        var result = assertDoesNotThrow(() -> terminologyService.getCriteriaProfileData(List.of()));
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "true, true, true",
        "true, true, false",
        "true, false, true",
        "true, false, false",
        "false, true, true",
        "false, true, false",
        "false, false, true",
        "false, false, false"
    })
    void getCriteriaProfileData(String noUiProfile, String noContext, String noTermcodes) throws IOException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");
        List<String> ids = List.of("123", "456", "789");
        boolean excludeUiProfile = Boolean.parseBoolean(noUiProfile);
        boolean excludeContext = Boolean.parseBoolean(noContext);
        boolean excludeTermcodes = Boolean.parseBoolean(noTermcodes);

        if (excludeUiProfile) {
            doReturn(Optional.empty()).when(uiProfileRepository).findByContextualizedTermcodeHash(any(String.class));
        } else {
            doReturn(Optional.of(createUiProfile())).when(uiProfileRepository).findByContextualizedTermcodeHash(any(String.class));
        }
        if (excludeContext) {
            doReturn(Optional.empty()).when(termCodeRepository).findContextByContextualizedTermcodeHash(any(String.class));
        } else {
            doReturn(Optional.of(createContext())).when(termCodeRepository).findContextByContextualizedTermcodeHash(any(String.class));
        }
        if (excludeTermcodes) {
            doReturn(Optional.empty()).when(termCodeRepository).findTermCodeByContextualizedTermcodeHash(any(String.class));
        } else {
            doReturn(Optional.of(createTermCode())).when(termCodeRepository).findTermCodeByContextualizedTermcodeHash(any(String.class));
        }

        var result = assertDoesNotThrow(() -> terminologyService.getCriteriaProfileData(ids));

        assertThat(result.size()).isEqualTo(ids.size());

        for (int i = 0; i < result.size(); i++) {
            CriteriaProfileData cpd = result.get(i);
            assertThat(cpd.getId()).isEqualTo(ids.get(i));
            assertThat(cpd.getContext() == null).isEqualTo(excludeContext);
            assertThat(cpd.getTermCodes().isEmpty()).isEqualTo(excludeTermcodes);
            assertThat(cpd.getUiProfile() == null).isEqualTo(excludeUiProfile);
        }
    }

    @Test
    void getTerminologySystems_succeeds() throws IOException, NoSuchFieldException, IllegalAccessException {
        var terminologyService = createTerminologyService("src/test/resources/ontology/ui_profiles");

        var terminologySystems = assertDoesNotThrow(terminologyService::getTerminologySystems);

        assertFalse(terminologySystems.isEmpty());
        assertThat(terminologySystems.size()).isEqualTo(4);
        assertThat(terminologySystems.get(0).name()).isEqualTo("loinc");
        assertThat(terminologySystems.get(2).url()).isEqualTo("http://fhir.de/CodeSystem/bfarm/ops");
    }

    private UiProfile createUiProfile() throws JsonProcessingException {
        var uiProfile = new UiProfile();
        uiProfile.setId(1);
        uiProfile.setName("example");
        uiProfile.setUiProfile(jsonUtil.writeValueAsString(
            de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.builder()
                .name("ExampleProfile")
                .timeRestrictionAllowed(true)
                .build())
        );
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

    private TermCode createTermCode() {
        TermCode termCode = new TermCode();
        termCode.setId(1);
        termCode.setCode("LL2191-6");
        termCode.setSystem("http://loinc.org");
        termCode.setVersion("1.0.0");
        termCode.setDisplay("Geschlecht");
        return termCode;
    }

    private Context createContext() {
        Context context = new Context();
        context.setId(1);
        context.setCode("LL2191-6");
        context.setSystem("http://loinc.org");
        context.setVersion("1.0.0");
        context.setDisplay("Geschlecht");
        return context;
    }
}
