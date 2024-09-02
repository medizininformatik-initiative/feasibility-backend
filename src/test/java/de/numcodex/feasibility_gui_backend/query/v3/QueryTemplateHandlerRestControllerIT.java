package de.numcodex.feasibility_gui_backend.query.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.status.ValidationIssue;
import de.numcodex.feasibility_gui_backend.query.api.validation.QueryTemplateValidatorSpringConfig;
import de.numcodex.feasibility_gui_backend.query.persistence.Query;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.AuthenticationHelper;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateException;
import de.numcodex.feasibility_gui_backend.terminology.validation.StructuredQueryValidation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("query")
@ExtendWith(SpringExtension.class)
@Import({QueryTemplateValidatorSpringConfig.class,
        RateLimitingServiceSpringConfig.class
})
@WebMvcTest(
        controllers = QueryTemplateHandlerRestController.class,
        properties = {
                "app.enableQueryValidation=true"
        }
)
public class QueryTemplateHandlerRestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper jsonUtil;

    @MockBean
    private QueryHandlerService queryHandlerService;

    @MockBean
    private StructuredQueryValidation structuredQueryValidation;

    @MockBean
    private AuthenticationHelper authenticationHelper;

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_succeedsWith201() throws Exception {
        long queryId = 1;
        doReturn(queryId).when(queryHandlerService).storeQueryTemplate(any(QueryTemplate.class), any(String.class));

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore(queryId))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", PATH_API + PATH_QUERY + PATH_TEMPLATE + "/" + queryId));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_failsOnInvalidQueryTemplate() throws Exception {
        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_failsOnTemplateExceptionWith500() throws Exception {
        doThrow(QueryTemplateException.class).when(queryHandlerService).storeQueryTemplate(any(QueryTemplate.class), any(String.class));

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore(1L))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_failsOnDuplicateWith409() throws Exception {
        doThrow(DataIntegrityViolationException.class).when(queryHandlerService).storeQueryTemplate(any(QueryTemplate.class), any(String.class));

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore(1L))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplate_succeeds() throws Exception {
        long queryTemplateId = 1;
        var annotatedQuery = createValidAnnotatedStructuredQuery(false);

        doReturn(createValidPersistenceQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).getQueryTemplate(any(Long.class), any(String.class));
        doReturn(createValidApiQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(annotatedQuery).when(structuredQueryValidation).annotateStructuredQuery(any(StructuredQuery.class), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE + "/" + queryTemplateId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queryTemplateId));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplate_failsOnNotFound() throws Exception {
        long queryTemplateId = 1;
        var annotatedQuery = createValidAnnotatedStructuredQuery(false);

        doThrow(QueryTemplateException.class).when(queryHandlerService).getQueryTemplate(any(Long.class), any(String.class));
        doReturn(createValidApiQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(annotatedQuery).when(structuredQueryValidation).annotateStructuredQuery(any(StructuredQuery.class), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE + "/" + queryTemplateId)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplate_failsOnJsonError() throws Exception {
        long queryTemplateId = 1;
        var annotatedQuery = createValidAnnotatedStructuredQuery(false);

        doReturn(createValidPersistenceQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).getQueryTemplate(any(Long.class), any(String.class));
        doThrow(JsonProcessingException.class).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(annotatedQuery).when(structuredQueryValidation).annotateStructuredQuery(any(StructuredQuery.class), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE + "/" + queryTemplateId)).with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplateList_succeeds() throws Exception {
        int listSize = 5;
        doReturn(createValidPersistenceQueryTemplateListToGet(listSize)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doReturn(createValidApiQueryTemplateToGet(ThreadLocalRandom.current().nextInt())).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE+ "?skipValidation=true")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(listSize))
                .andExpect(jsonPath("$.[0].id").exists());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplateList_emptyListOnJsonErrors() throws Exception {
        doReturn(createValidPersistenceQueryTemplateListToGet(5)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doThrow(JsonProcessingException.class).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplateListWithValidation_succeeds(boolean isValid) throws Exception {
        int listSize = 5;
        doReturn(createValidPersistenceQueryTemplateListToGet(listSize)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doReturn(createValidApiQueryTemplateToGet(ThreadLocalRandom.current().nextInt())).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(isValid).when(structuredQueryValidation).isValid(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(listSize))
            .andExpect(jsonPath("$.[*].id").exists())
            .andExpect(jsonPath("$.[*].isValid").exists())
            .andExpect(jsonPath("$.[*].isValid", everyItem(equalTo(isValid))));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplateListWithValidation_emptyListOnJsonErrors() throws Exception {
        int listSize = 5;
        doReturn(createValidPersistenceQueryTemplateListToGet(listSize)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doThrow(JsonProcessingException.class).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(false).when(structuredQueryValidation).isValid(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testUpdateQueryTemplate_succeeds() throws Exception {
        doNothing().when(queryHandlerService).updateQueryTemplate(any(Long.class), any(QueryTemplate.class), any(String.class));

        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE + "/1")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testUpdateQueryTemplate_failsOnInvalidQueryTemplate() throws Exception {
        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE + "/1")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testUpdateQueryTemplate_failsOnTemplateExceptionWith404() throws Exception {
        doThrow(QueryTemplateException.class).when(queryHandlerService).updateQueryTemplate(any(Long.class), any(QueryTemplate.class), any(String.class));

        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE+ "/1")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore(1L))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testDeleteQueryTemplate_succeeds() throws Exception {
        doNothing().when(queryHandlerService).deleteQueryTemplate(any(Long.class), any(String.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE + "/1")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testDeleteQueryTemplate_failsWith404OnNotFound() throws Exception {
        doThrow(QueryTemplateException.class).when(queryHandlerService).deleteQueryTemplate(any(Long.class), any(String.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + PATH_TEMPLATE + "/1")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @NotNull
    private static QueryTemplate createValidQueryTemplateToStore(long id) {
        return QueryTemplate.builder()
                .id(id)
                .content(createValidStructuredQuery())
                .label("TestLabel")
                .comment("TestComment")
                .isValid(true)
                .build();
    }

    @NotNull
    private static de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate createValidPersistenceQueryTemplateToGet(long id) {
        var queryTemplate = new de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate();
        queryTemplate.setId(id);
        queryTemplate.setQuery(new Query());
        queryTemplate.setLabel("TestLabel");
        queryTemplate.setComment("TestComment");
        queryTemplate.setLastModified(new Timestamp(new java.util.Date().getTime()));
        return queryTemplate;
    }

    @NotNull
    private static List<de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate> createValidPersistenceQueryTemplateListToGet(int entries) {
        var queryTemplateList = new ArrayList<de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate>();
        for (int i = 0; i < entries; ++i) {
            queryTemplateList.add(createValidPersistenceQueryTemplateToGet(i));
        }
        return queryTemplateList;
    }

    @NotNull
    private static QueryTemplate createValidApiQueryTemplateToGet(long id) {
        return QueryTemplate.builder()
                .id(id)
                .content(createValidStructuredQuery())
                .label("TestLabel")
                .comment("TestComment")
                .lastModified(new Timestamp(new java.util.Date().getTime()).toString())
                .createdBy("someone")
                .isValid(true)
                .build();
    }

    @NotNull
    private static StructuredQuery createValidStructuredQuery() {
        var inclusionCriterion = Criterion.builder()
                .termCodes(List.of(createTermCode()))
                .attributeFilters(List.of())
                .context(createTermCode())
                .build();
        return StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(inclusionCriterion)))
                .exclusionCriteria(List.of())
                .display("foo")
                .build();
    }

    @NotNull
    private static StructuredQuery createValidAnnotatedStructuredQuery(boolean withIssues) {
        var termCode = TermCode.builder()
            .code("LL2191-6")
            .system("http://loinc.org")
            .display("Geschlecht")
            .build();
        var inclusionCriterion = Criterion.builder()
            .termCodes(List.of(termCode))
            .attributeFilters(List.of())
            .context(termCode)
            .validationIssues(withIssues ? List.of(ValidationIssue.TERMCODE_CONTEXT_COMBINATION_INVALID) : List.of())
            .build();
        return StructuredQuery.builder()
            .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
            .inclusionCriteria(List.of(List.of(inclusionCriterion)))
            .exclusionCriteria(List.of())
            .display("foo")
            .build();
    }

    @NotNull
    private static TermCode createTermCode() {
        return TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();
    }

    @NotNull
    private static Criterion createInvalidCriterion() {
        return Criterion.builder()
            .context(null)
            .termCodes(List.of(createTermCode()))
            .build();
    }
}
