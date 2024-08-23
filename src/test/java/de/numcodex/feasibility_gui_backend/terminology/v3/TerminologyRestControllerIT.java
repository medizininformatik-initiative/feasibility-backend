package de.numcodex.feasibility_gui_backend.terminology.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.MappingNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.NodeNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.UiProfileNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.api.*;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
    private RateLimitingInterceptor rateLimitingInterceptor;

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
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
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetCriteriaProfileData_succeedsWith200OnEmptyList() throws Exception {
        doReturn(List.of()).when(terminologyService).getCriteriaProfileData(anyList());

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/criteria-profile-data")).param("ids", "123").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.*", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetEntry_succeedsWith200() throws Exception {
        var id = UUID.randomUUID();
        var terminologyEntry = createTerminologyEntry(id);
        doReturn(terminologyEntry).when(terminologyService).getEntry(any(UUID.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entries/" + id)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.context.code").value(terminologyEntry.getContext().code()))
                .andExpect(jsonPath("$.context.system").value(terminologyEntry.getContext().system()))
                .andExpect(jsonPath("$.context.display").value(terminologyEntry.getContext().display()))
                .andExpect(jsonPath("$.termCodes.[0].code").value(terminologyEntry.getTermCodes().get(0).code()))
                .andExpect(jsonPath("$.termCodes.[0].system").value(terminologyEntry.getTermCodes().get(0).system()))
                .andExpect(jsonPath("$.termCodes.[0].display").value(terminologyEntry.getTermCodes().get(0).display()));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetEntry_failsWith404OnNotFound() throws Exception {
        Mockito.doThrow(NodeNotFoundException.class).when(terminologyService).getEntry(any(UUID.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entries/" + UUID.randomUUID())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetCategories_succeedsWith200() throws Exception {
        List<CategoryEntry> categoryEntries = createCategoryEntries();
        doReturn(categoryEntries).when(terminologyService).getCategories();

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/categories")).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(categoryEntries.size()))
                .andExpect(jsonPath("$.[*].catId").exists())
                .andExpect(jsonPath("$.[*].display").exists())
                .andExpect(jsonPath("$[*].display", Matchers.everyItem(Matchers.containsString("category-"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetSelectableEntries_succeedsWith200(boolean includeCategory) throws Exception {
        var id = UUID.randomUUID();
        var terminologyEntries = List.of(createTerminologyEntry(id));
        MockHttpServletRequestBuilder requestBuilder;
        doReturn(terminologyEntries).when(terminologyService).getSelectableEntries(any(String.class), any(UUID.class));
        doReturn(terminologyEntries).when(terminologyService).getSelectableEntries(any(String.class), isNull());

        if (includeCategory) {
            requestBuilder = get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entries"))
                    .param("query", "GESCH")
                    .param("categoryId", UUID.randomUUID().toString())
                    .with(csrf());

        } else {
            requestBuilder = get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entries"))
                    .param("query", "GESCH")
                    .with(csrf());

        }
        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(id.toString()))
                .andExpect(jsonPath("$.[0].context.code").value(terminologyEntries.get(0).getContext().code()))
                .andExpect(jsonPath("$.[0].context.system").value(terminologyEntries.get(0).getContext().system()))
                .andExpect(jsonPath("$.[0].context.display").value(terminologyEntries.get(0).getContext().display()))
                .andExpect(jsonPath("$.[0].termCodes.[0].code").value(terminologyEntries.get(0).getTermCodes().get(0).code()))
                .andExpect(jsonPath("$.[0].termCodes.[0].system").value(terminologyEntries.get(0).getTermCodes().get(0).system()))
                .andExpect(jsonPath("$.[0].termCodes.[0].display").value(terminologyEntries.get(0).getTermCodes().get(0).display()));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetSelectableEntries_succeedsWith200ButEmptyOnNoMatch() throws Exception {
        doReturn(List.of()).when(terminologyService).getSelectableEntries(any(String.class), any(UUID.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entries"))
                        .param("query", "GESCH")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetSelectableEntries_failsWith400OnMissingQueryParameter() throws Exception {
        doReturn(List.of()).when(terminologyService).getSelectableEntries(any(String.class), any(UUID.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_TERMINOLOGY + "/entries"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetUiProfile_succeedsWith200() throws Exception {
        MockHttpServletRequestBuilder requestBuilder;
        doReturn(createUiProfileString()).when(terminologyService).getUiProfile(any(String.class));

        requestBuilder = get(URI.create(PATH_API + PATH_TERMINOLOGY + "/5e6679ac-2b20-48ad-8459-f102c8944a06/ui_profile"))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string(createUiProfileString()));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetUiProfile_failsWith404OnNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder;
        doThrow(UiProfileNotFoundException.class).when(terminologyService).getUiProfile(any(String.class));

        requestBuilder = get(URI.create(PATH_API + PATH_TERMINOLOGY + "/5e6679ac-2b20-48ad-8459-f102c8944a06/ui_profile"))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetMapping_succeedsWith200() throws Exception {
        MockHttpServletRequestBuilder requestBuilder;
        doReturn(createUiProfileString()).when(terminologyService).getMapping(any(String.class));

        requestBuilder = get(URI.create(PATH_API + PATH_TERMINOLOGY + "/5e6679ac-2b20-48ad-8459-f102c8944a06/mapping"))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string(createUiProfileString()));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetMapping_failsWith404OnNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder;
        doThrow(MappingNotFoundException.class).when(terminologyService).getMapping(any(String.class));

        requestBuilder = get(URI.create(PATH_API + PATH_TERMINOLOGY + "/5e6679ac-2b20-48ad-8459-f102c8944a06/mapping"))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetIntersection_succeedsWith200() throws Exception {
        MockHttpServletRequestBuilder requestBuilder;
        var randomUuidList = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        var randomUuidListMinusOne = randomUuidList.subList(0, randomUuidList.size() - 2);
        doReturn(randomUuidListMinusOne).when(terminologyService).getIntersection(any(String.class), anyList());

        requestBuilder = post(URI.create(PATH_API + PATH_TERMINOLOGY + "/criteria-set/intersect"))
                .param("criteriaSetUrl", "http://foo.bar")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .content(jsonUtil.writeValueAsString(randomUuidList))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().json(jsonUtil.writeValueAsString(randomUuidListMinusOne)));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
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

    private List<CategoryEntry> createCategoryEntries() {
        return List.of(new CategoryEntry(UUID.randomUUID(), "category-1"),
                new CategoryEntry(UUID.randomUUID(), "category-2"),
                new CategoryEntry(UUID.randomUUID(), "category-3"),
                new CategoryEntry(UUID.randomUUID(), "category-4"),
                new CategoryEntry(UUID.randomUUID(), "category-5"));
    }

    private TerminologyEntry createTerminologyEntry(UUID id) {
        TerminologyEntry terminologyEntry = new TerminologyEntry();
        terminologyEntry.setId(id);
        terminologyEntry.setContext(createTermCode());
        terminologyEntry.setTermCodes(List.of(createTermCode()));
        terminologyEntry.setChildren(List.of());
        terminologyEntry.setLeaf(true);
        terminologyEntry.setSelectable(true);
        terminologyEntry.setDisplay("TerminologyEntry");
        terminologyEntry.setRoot(false);
        return terminologyEntry;
    }

    private TermCode createTermCode() {
        return TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .version("1.0.0")
                .build();
    }

    private String createUiProfileString() throws JsonProcessingException {
        var uiProfile = UiProfile.builder()
            .name("Diagnose")
            .timeRestrictionAllowed(true)
            .build();
        return jsonUtil.writeValueAsString(uiProfile);
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
}
