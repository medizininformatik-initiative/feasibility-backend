package de.numcodex.feasibility_gui_backend.terminology.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Comparator;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.TerminologyService;
import de.numcodex.feasibility_gui_backend.terminology.api.*;
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
}
