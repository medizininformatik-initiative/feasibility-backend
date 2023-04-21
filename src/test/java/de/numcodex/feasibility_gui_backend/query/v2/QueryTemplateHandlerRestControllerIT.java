package de.numcodex.feasibility_gui_backend.query.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.validation.QueryTemplateValidatorSpringConfig;
import de.numcodex.feasibility_gui_backend.query.persistence.Query;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.AuthenticationHelper;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateException;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private TermCodeValidation termCodeValidation;

    @MockBean
    private AuthenticationHelper authenticationHelper;

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_succeedsWith201() throws Exception {
        long queryId = 1;
        doReturn(queryId).when(queryHandlerService).storeQueryTemplate(any(QueryTemplate.class), any(String.class));

        mockMvc.perform(post(URI.create("/api/v2/query/template")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", "/api/v2/query/template/" + queryId));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_failsOnInvalidQueryTemplate() throws Exception {
        mockMvc.perform(post(URI.create("/api/v2/query/template")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_failsOnTemplateExceptionWith500() throws Exception {
        doThrow(QueryTemplateException.class).when(queryHandlerService).storeQueryTemplate(any(QueryTemplate.class), any(String.class));

        mockMvc.perform(post(URI.create("/api/v2/query/template")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testStoreQueryTemplate_failsOnDuplicateWith409() throws Exception {
        doThrow(DataIntegrityViolationException.class).when(queryHandlerService).storeQueryTemplate(any(QueryTemplate.class), any(String.class));

        mockMvc.perform(post(URI.create("/api/v2/query/template")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(createValidQueryTemplateToStore())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplate_succeeds() throws Exception {
        long queryTemplateId = 1;

        doReturn(createValidPersistenceQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).getQueryTemplate(any(Long.class), any(String.class));
        doReturn(createValidApiQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template/" + queryTemplateId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queryTemplateId));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplate_failsOnNotFound() throws Exception {
        long queryTemplateId = 1;

        doThrow(QueryTemplateException.class).when(queryHandlerService).getQueryTemplate(any(Long.class), any(String.class));
        doReturn(createValidApiQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template/" + queryTemplateId)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplate_failsOnJsonError() throws Exception {
        long queryTemplateId = 1;

        doReturn(createValidPersistenceQueryTemplateToGet(queryTemplateId)).when(queryHandlerService).getQueryTemplate(any(Long.class), any(String.class));
        doThrow(JsonProcessingException.class).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template/" + queryTemplateId)).with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplateList_succeeds() throws Exception {
        int listSize = 5;
        doReturn(createValidPersistenceQueryTemplateListToGet(listSize)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doReturn(createValidApiQueryTemplateToGet(ThreadLocalRandom.current().nextInt())).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(listSize))
                .andExpect(jsonPath("$.[0].id").exists());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testGetQueryTemplateList_emptyListOnJsonErrors() throws Exception {
        doReturn(createValidPersistenceQueryTemplateListToGet(5)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doThrow(JsonProcessingException.class).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testValidateTemplates_succeedsWithoutValidationErrors() throws Exception {
        int listSize = 5;
        doReturn(createValidPersistenceQueryTemplateListToGet(listSize)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doReturn(createValidApiQueryTemplateToGet(ThreadLocalRandom.current().nextInt())).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template/validate")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(listSize))
                .andExpect(jsonPath("$.[*].id").exists())
                .andExpect(jsonPath("$.[*].isValid", Matchers.not(Matchers.contains(false))));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testValidateTemplates_succeedsWithValidationErrors() throws Exception {
        int listSize = 5;
        doReturn(createValidPersistenceQueryTemplateListToGet(listSize)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doReturn(createValidApiQueryTemplateToGet(ThreadLocalRandom.current().nextInt())).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(List.of(createTermCode())).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template/validate")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(listSize));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testValidateTemplates_emptyListOnJsonErrors() throws Exception {
        int listSize = 5;
        doReturn(createValidPersistenceQueryTemplateListToGet(listSize)).when(queryHandlerService).getQueryTemplatesForAuthor(any(String.class));
        doThrow(JsonProcessingException.class).when(queryHandlerService).convertTemplatePersistenceToApi(any(de.numcodex.feasibility_gui_backend.query.persistence.QueryTemplate.class));
        doReturn(List.of(createTermCode())).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create("/api/v2/query/template/validate")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @NotNull
    private static QueryTemplate createValidQueryTemplateToStore() {
        var queryTemplate = new QueryTemplate();
        queryTemplate.setContent(createValidStructuredQuery());
        queryTemplate.setLabel("TestLabel");
        queryTemplate.setComment("TestComment");
        return queryTemplate;
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
        var queryTemplate = new QueryTemplate();
        queryTemplate.setId(id);
        queryTemplate.setContent(createValidStructuredQuery());
        queryTemplate.setLabel("TestLabel");
        queryTemplate.setComment("TestComment");
        queryTemplate.setLastModified(new Timestamp(new java.util.Date().getTime()).toString());
        queryTemplate.setCreatedBy("someone");
        queryTemplate.setInvalidTerms(List.of());
        queryTemplate.setIsValid(true);
        return queryTemplate;
    }

    @NotNull
    private static StructuredQuery createValidStructuredQuery() {
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
        return testQuery;
    }

    @NotNull
    private static TermCode createTermCode() {
        var termCode = new TermCode();
        termCode.setCode("LL2191-6");
        termCode.setSystem("http://loinc.org");
        termCode.setDisplay("Geschlecht");
        return termCode;
    }
}
