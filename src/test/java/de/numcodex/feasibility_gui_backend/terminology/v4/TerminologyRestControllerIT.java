package de.numcodex.feasibility_gui_backend.terminology.v4;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.api.*;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_API;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_TERMINOLOGY;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("terminology")
@ExtendWith(SpringExtension.class)
@Import(RateLimitingServiceSpringConfig.class)
@WebMvcTest(
        controllers = TerminologyRestController.class
)
public class TerminologyRestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper jsonUtil;

    @MockBean
    private StructuredQueryValidation structuredQueryValidation;

    @MockBean
    private TerminologyService terminologyService;

    @MockBean
    private TerminologyEsService terminologyEsService;

    @MockBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetCriteriaProfileData_succeedsWith200() throws Exception {
        var id = UUID.randomUUID();
        var criteriaProfileDataList = createCriteriaProfileDataList(List.of(id));
        doReturn(criteriaProfileDataList).when(terminologyService).getCriteriaProfileData(anyList());

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/criteria-profile-data")).param("ids", id.toString()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.*", hasSize(1)))
            .andExpect(jsonPath("$.[0].id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetCriteriaProfileData_succeedsWith200OnEmptyList() throws Exception {
        doReturn(List.of()).when(terminologyService).getCriteriaProfileData(anyList());

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/criteria-profile-data")).param("ids", "123").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.*", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetTerminologySystems_succeedsWith200() throws Exception {
        MockHttpServletRequestBuilder requestBuilder;
        doReturn(List.of(TerminologySystemEntry.builder().url("http://foo.bar").name("Foobar").build())).when(terminologyService).getTerminologySystems();

        requestBuilder = get(URI.create(PATH_API + PATH_TERMINOLOGY + "/systems"))
            .with(csrf());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].url").value("http://foo.bar"))
            .andExpect(jsonPath("$[0].name").value("Foobar"));
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetFilters_succeeds() throws Exception {
        List<String> filterList = List.of("context", "kdsModule", "terminology");
        List<TermFilter> termFilterList = createTermFilterList(filterList.toArray(new String[0]));
        doReturn(termFilterList).when(terminologyEsService).getAvailableFilters();

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/search/filter")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().json(jsonUtil.writeValueAsString(termFilterList)));
    }

    @Test
    public void testGetFilters_failsOnUnauthorized() throws Exception {
        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/search/filter")).with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testSearchOntologyItemsCriteriaQuery_succeeds() throws Exception {
        var totalHits = 1;
        var dummyEsSearchResult = createDummyEsSearchResult(totalHits);
        doReturn(dummyEsSearchResult).when(terminologyEsService).performOntologySearchWithPaging(any(String.class), isNull(), isNull(), isNull(), isNull(), anyBoolean(), anyInt(), anyInt());

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entry/search")).param("searchterm", "some-context").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalHits").value(dummyEsSearchResult.totalHits()))
            .andExpect(jsonPath("$.results[0].id").value(dummyEsSearchResult.results().get(0).id()))
            .andExpect(jsonPath("$.results[0].name").value(dummyEsSearchResult.results().get(0).name()))
            .andExpect(jsonPath("$.results[0].terminology").value(dummyEsSearchResult.results().get(0).terminology()))
            .andExpect(jsonPath("$.results[0].selectable").value(dummyEsSearchResult.results().get(0).selectable()))
            .andExpect(jsonPath("$.results[0].kdsModule").value(dummyEsSearchResult.results().get(0).kdsModule()))
            .andExpect(jsonPath("$.results[0].availability").value(dummyEsSearchResult.results().get(0).availability()))
            .andExpect(jsonPath("$.results[0].context").value(dummyEsSearchResult.results().get(0).context()));

    }

    @Test
    public void testSearchOntologyItemsCriteriaQuery_failsOnUnauthorized() throws Exception {
        doReturn(createDummyEsSearchResult(1)).when(terminologyEsService).performOntologySearchWithPaging(any(String.class), anyList(), anyList(), anyList(), anyList(), any(Boolean.class), any(Integer.class), any(Integer.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entry/search")).param("searchterm", "some-context").with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetOntologyItemRelationsByHash_succeeds() throws Exception {
        var dummyOntologyItemRelations = createDummyOntologyItemRelations();
        doReturn(dummyOntologyItemRelations).when(terminologyEsService).getOntologyItemRelationsByHash(any(String.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entry/abc/relations")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.translations[0].lang").value(dummyOntologyItemRelations.translations().stream().toList().get(0).lang()))
            .andExpect(jsonPath("$.translations[0].value").value(dummyOntologyItemRelations.translations().stream().toList().get(0).value()))
            .andExpect(jsonPath("$.children[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.children().stream().toList().get(0).contextualizedTermcodeHash()))
            .andExpect(jsonPath("$.children[0].name").value(dummyOntologyItemRelations.children().stream().toList().get(0).name()))
            .andExpect(jsonPath("$.parents[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.parents().stream().toList().get(0).contextualizedTermcodeHash()))
            .andExpect(jsonPath("$.parents[0].name").value(dummyOntologyItemRelations.parents().stream().toList().get(0).name()))
            .andExpect(jsonPath("$.relatedTerms[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.relatedTerms().stream().toList().get(0).contextualizedTermcodeHash()))
            .andExpect(jsonPath("$.relatedTerms[0].name").value(dummyOntologyItemRelations.relatedTerms().stream().toList().get(0).name()));
    }

    @Test
    public void testGetOntologyItemRelationsByHash_failsOnUnauthorized() throws Exception {
        var dummyOntologyItemRelations = createDummyOntologyItemRelations();
        doReturn(dummyOntologyItemRelations).when(terminologyEsService).getOntologyItemRelationsByHash(any(String.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entry/abc/relations")).with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DATAPORTAL_TEST_USER")
    public void testGetOntologyItemByHash_succeeds() throws Exception {
        var dummySearchResultEntry = createDummyEsSearchResultEntry();
        doReturn(dummySearchResultEntry).when(terminologyEsService).getSearchResultEntryByHash(any(String.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entry/abc")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(dummySearchResultEntry.id()))
            .andExpect(jsonPath("$.name").value(dummySearchResultEntry.name()))
            .andExpect(jsonPath("$.availability").value(dummySearchResultEntry.availability()))
            .andExpect(jsonPath("$.context").value(dummySearchResultEntry.context()))
            .andExpect(jsonPath("$.terminology").value(dummySearchResultEntry.terminology()))
            .andExpect(jsonPath("$.termcode").value(dummySearchResultEntry.termcode()))
            .andExpect(jsonPath("$.kdsModule").value(dummySearchResultEntry.kdsModule()))
            .andExpect(jsonPath("$.selectable").value(dummySearchResultEntry.selectable()));
    }

    @Test
    public void testGetOntologyItemByHash_failsOnUnauthorized() throws Exception {
        var dummySearchResultEntry = createDummyEsSearchResultEntry();
        doReturn(dummySearchResultEntry).when(terminologyEsService).getSearchResultEntryByHash(any(String.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entry/abc")).with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    private TermCode createTermCode() {
        return TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .version("1.0.0")
                .build();
    }

    private UiProfile createUiProfile() {
        return UiProfile.builder()
            .name("test-ui-profile")
            .attributeDefinitions(List.of(createAttributeDefinition()))
            .valueDefinition(createAttributeDefinition())
            .timeRestrictionAllowed(true)
            .build();
    }

    private AttributeDefinition createAttributeDefinition() {
        return AttributeDefinition.builder()
            .min(1.0)
            .max(99.9)
            .allowedUnits(List.of(createTermCode()))
            .attributeCode(createTermCode())
            .type(ValueDefinitonType.CONCEPT)
            .optional(false)
            .referencedCriteriaSet("http://my.reference.criteria/set")
            .referencedValueSet("http://my.reference.value/set")
            .comparator(Comparator.EQUAL)
            .precision(1.0)
            .selectableConcepts(List.of(createTermCode()))
            .build();
    }

    private List<CriteriaProfileData> createCriteriaProfileDataList(List<UUID> ids) {
        List<CriteriaProfileData> criteriaProfileDataList = new ArrayList<>();
        for (UUID uuid: ids) {
            criteriaProfileDataList.add(
                CriteriaProfileData.builder()
                    .id(uuid.toString())
                    .context(createTermCode())
                    .termCodes(List.of(createTermCode()))
                    .uiProfile(createUiProfile())
                    .build()
            );
        }
        return criteriaProfileDataList;
    }

    private EsSearchResult createDummyEsSearchResult(int totalHits) {
        return EsSearchResult.builder()
            .totalHits(totalHits)
            .results(List.of(createDummyEsSearchResultEntry()))
            .build();
    }

    private EsSearchResultEntry createDummyEsSearchResultEntry() {
        return EsSearchResultEntry.builder()
            .terminology("some-terminology")
            .availability(100)
            .context("some-context")
            .id("abc-123")
            .kdsModule("some-module")
            .name("some-name")
            .selectable(true)
            .build();
    }

    private OntologyItemRelationsDocument createDummyOntologyItemRelations() {
        return OntologyItemRelationsDocument.builder()
            .relatedTerms(List.of(createDummyRelative()))
            .translations(List.of(createDummyTranslation()))
            .parents(List.of(createDummyRelative()))
            .children(List.of(createDummyRelative()))
            .build();
    }

    private Translation createDummyTranslation() {
        return Translation.builder()
            .lang("de")
            .value("Lorem Ipsum")
            .build();
    }

    private Relative createDummyRelative() {
        return Relative.builder()
            .contextualizedTermcodeHash(UUID.randomUUID().toString())
            .name("some-random-name")
            .build();
    }

    private List<TermFilter> createTermFilterList(String[] values) {
        var termFilters = new ArrayList<TermFilter>();
        for (String term : values) {
            termFilters.add(
                TermFilter.builder()
                    .name(term)
                    .type("selectable-concept")
                    .values(List.of(TermFilterValue.builder().label("baz").count(values.length).build()))
                    .build()
            );
        }
        termFilters.add(TermFilter.builder()
            .type("boolean")
            .name("availability")
            .values(List.of())
            .build());
        return termFilters;
    }
}
