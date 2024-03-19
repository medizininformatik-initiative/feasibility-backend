package de.numcodex.feasibility_gui_backend.query.v3;
import de.numcodex.feasibility_gui_backend.config.WebSecurityConfig;
import de.numcodex.feasibility_gui_backend.query.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.api.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.status.FeasibilityIssue;
import de.numcodex.feasibility_gui_backend.query.api.validation.StructuredQueryValidatorSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.persistence.UserBlacklist;
import de.numcodex.feasibility_gui_backend.query.persistence.UserBlacklistRepository;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.AuthenticationHelper;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.query.result.ResultLine;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import java.sql.Timestamp;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_API;
import static de.numcodex.feasibility_gui_backend.config.WebSecurityConfig.PATH_QUERY;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("query")
@ExtendWith(SpringExtension.class)
@Import({StructuredQueryValidatorSpringConfig.class,
        RateLimitingServiceSpringConfig.class
})
@WebMvcTest(
        controllers = QueryHandlerRestController.class,
        properties = {
                "app.enableQueryValidation=true"
        }
)
@SuppressWarnings("NewClassNamingConvention")
public class QueryHandlerRestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper jsonUtil;

    @MockBean
    private QueryHandlerService queryHandlerService;

    @MockBean
    private TermCodeValidation termCodeValidation;

    @MockBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    @MockBean
    private UserBlacklistRepository userBlacklistRepository;

    @MockBean
    private AuthenticationHelper authenticationHelper;

    @Value("${app.privacy.quota.soft.create.amount}")
    private int quotaSoftCreateAmount;

    @Value("${app.privacy.quota.soft.create.intervalminutes}")
    private int quotaSoftCreateIntervalMinutes;

    @Value("${app.privacy.quota.hard.create.amount}")
    private int quotaHardCreateAmount;

    @Value("${app.privacy.quota.hard.create.intervalminutes}")
    private int quotaHardCreateIntervalMinutes;

    @Value("${app.privacy.threshold.sitesResult}")
    private long thresholdSitesResult;

    @Value("${app.maxSavedQueriesPerUser}")
    private long maxSavedQueriesPerUser;

    @BeforeEach
    void initTest() throws Exception {
        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testRunQueryEndpoint_FailsOnInvalidStructuredQueryWith400() throws Exception {
        var testQuery = StructuredQuery.builder().build();

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_SucceedsOnValidStructuredQueryWith201() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        var mvcResult = mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", PATH_API + PATH_QUERY + "/1"));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_FailsOnDownstreamServiceError() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        var dispatchError = new QueryDispatchException("something went wrong");

        doReturn(Mono.error(dispatchError)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        var mvcResult = mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_FailsOnSoftQuotaExceeded() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        doReturn((long)quotaSoftCreateAmount + 1).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), any(Integer.class));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        var mvcResult = mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY)).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testValidateQueryEndpoint_SucceedsOnValidQuery() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + "/validate")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testValidateQueryEndpoint_FailsOnInvalidStructuredQueryWith400() throws Exception {
        var testQuery = StructuredQuery.builder().build();

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + "/validate")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_FailsOnBeingBlacklistedWith403() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();
        UserBlacklist userBlacklistEntry = new UserBlacklist();
        userBlacklistEntry.setId(1L);
        userBlacklistEntry.setUserId("test");
        userBlacklistEntry.setBlacklistedAt(new Timestamp(System.currentTimeMillis()));
        Optional<UserBlacklist> userBlacklistOptional = Optional.of(userBlacklistEntry);
        doReturn(userBlacklistOptional).when(userBlacklistRepository).findByUserId(any(String.class));
        doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        var mvcResult = mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY)).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_FailsOnExceedingHardLimitWith403() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        doReturn((long)quotaHardCreateAmount).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaHardCreateIntervalMinutes));
        doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        var mvcResult = mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY)).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER", "FEASIBILITY_TEST_POWER"}, username = "test")
    public void testRunQueryEndpoint_SucceedsOnExceedingHardlimitAsPowerUserWith201() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("FEASIBILITY_TEST_POWER"));
        doReturn((long)quotaHardCreateAmount).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaHardCreateIntervalMinutes));
        doReturn((long)(quotaSoftCreateAmount - 1)).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaSoftCreateIntervalMinutes));
        doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        var mvcResult = mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY)).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isCreated())
            .andExpect(header().exists("location"))
            .andExpect(header().string("location", PATH_API + PATH_QUERY + "/1"));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQueryList_SucceedsWithValidation() throws Exception {
        long queryId = 1;
        doReturn(List.of(createValidQuery(queryId))).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of(createValidQueryListEntry(queryId, false))).when(queryHandlerService).convertQueriesToQueryListEntries(anyList(), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY)).with(csrf()).param("skipValidation", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(queryId))
                .andExpect(jsonPath("$[0].invalidTerms").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQueryList_SucceedsWithoutValidation() throws Exception {
        long queryId = 1;
        doReturn(List.of(createValidQuery(queryId))).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of(createValidQueryListEntry(queryId, true))).when(queryHandlerService).convertQueriesToQueryListEntries(anyList(), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY)).with(csrf()).param("skipValidation", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(queryId))
            .andExpect(jsonPath("$[0].invalidTerms").isArray())
            .andExpect(jsonPath("$[0].invalidTerms").isEmpty());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQueryList_SucceedsWithoutDefiningSkipValidation() throws Exception {
        long queryId = 1;
        doReturn(List.of(createValidQuery(queryId))).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of(createValidQueryListEntry(queryId, false))).when(queryHandlerService).convertQueriesToQueryListEntries(anyList(), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(queryId))
            .andExpect(jsonPath("$[0].invalidTerms").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_ADMIN"}, username = "test")
    public void testGetQueryListForUser_SucceedsOnValidUser() throws Exception {
        long queryId = 1;
        String userId = "user1";
        doReturn(List.of(createValidQuery(queryId))).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of(createValidQueryListEntry(queryId, false))).when(queryHandlerService).convertQueriesToQueryListEntries(anyList(), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/by-user/" + userId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(queryId));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_ADMIN"}, username = "test")
    public void testGetQueryListForUser_ReturnsEmptyOnUnknownUser() throws Exception {
        String userId = "user1";
        doReturn(List.of()).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of()).when(queryHandlerService).convertQueriesToQueryListEntries(anyList(), any(Boolean.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/by-user/" + userId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQuery_succeeds() throws Exception {
        long queryId = 1;
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createValidApiQuery(queryId)).when(queryHandlerService).getQuery(any(Long.class));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/" + queryId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queryId));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQuery_failsOnWrongAuthorWith403() throws Exception {
        long queryId = 1;
        doReturn("some-other-user").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createValidApiQuery(queryId)).when(queryHandlerService).getQuery(any(Long.class));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/" + queryId)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_Succeeds() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_failsWith404OnAuthorForQueryNotFound() throws Exception {
        doThrow(QueryNotFoundException.class).when(queryHandlerService).getAuthorId(any(Long.class));

        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_failsWith403OnAuthorMismatch() throws Exception {
        doReturn("SomeOtherUser").when(queryHandlerService).getAuthorId(any(Long.class));

        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_failsWith409OnExistingSavedQuery() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doThrow(DataIntegrityViolationException.class).when(queryHandlerService).saveQuery(any(Long.class), any(String.class), any(SavedQuery.class));

        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_failsWith403OnNoFreeSlots() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(maxSavedQueriesPerUser).when(queryHandlerService).getAmountOfSavedQueriesByUser(any(String.class));

        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(post(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testUpdateSavedQuery_Succeeds() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testUpdateSavedQuery_failsWith403OnAuthorMismatch() throws Exception {
        doReturn("SomeOtherUser").when(queryHandlerService).getAuthorId(any(Long.class));

        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testUpdateSavedQuery_failsWith404OnAuthorForQueryNotFound() throws Exception {
        doThrow(QueryNotFoundException.class).when(queryHandlerService).getAuthorId(any(Long.class));

        var savedQuery = SavedQuery.builder()
                .label("foo")
                .comment("bar")
                .totalNumberOfPatients(100L)
                .build();

        mockMvc.perform(put(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testDeleteSavedQuery_Succeeds() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testDeleteSavedQuery_FailsWith403OnWrongAuthor() throws Exception {
        doReturn("some-other-user").when(queryHandlerService).getAuthorId(any(Long.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testDeleteSavedQuery_FailsWith404IfQueryNotFound() throws Exception {
        doThrow(QueryNotFoundException.class).when(queryHandlerService).getAuthorId(any(Long.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testDeleteSavedQuery_FailsWith404IfSavedQueryNotFound() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doThrow(QueryNotFoundException.class).when(queryHandlerService).deleteSavedQuery(any(Long.class));

        mockMvc.perform(delete(URI.create(PATH_API + PATH_QUERY + "/1/saved")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource
    @WithMockUser(roles = {"FEASIBILITY_TEST_ADMIN"}, username = "test")
    public void testGetQueryResult_succeeds(QueryHandlerService.ResultDetail resultDetail) throws Exception {
        var requestUri = PATH_API + PATH_QUERY + "/1";
        doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("FEASIBILITY_TEST_ADMIN"));
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createTestQueryResult(resultDetail)).when(queryHandlerService).getQueryResult(any(Long.class), any(QueryHandlerService.ResultDetail.class));

        switch (resultDetail) {
            case SUMMARY -> {
                requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
            }
            case DETAILED_OBFUSCATED -> {
                requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
            }
            case DETAILED -> {
                requestUri = requestUri + WebSecurityConfig.PATH_DETAILED_RESULT;
            }
        }

        switch (resultDetail) {
            case SUMMARY -> {
                mockMvc.perform(get(requestUri).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalNumberOfPatients").exists())
                        .andExpect(jsonPath("$.resultLines", empty()));
            }
            case DETAILED_OBFUSCATED -> {
                mockMvc.perform(get(requestUri).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalNumberOfPatients").exists())
                        .andExpect(jsonPath("$.resultLines").exists())
                        .andExpect(jsonPath("$.resultLines[0].siteName", startsWith("foobar")));
            }
            case DETAILED -> {
                mockMvc.perform(get(requestUri).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalNumberOfPatients").exists())
                        .andExpect(jsonPath("$.resultLines").exists())
                        .andExpect(jsonPath("$.resultLines[0].siteName", not(startsWith("foobar"))));
            }
        }
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetDetailedObfuscatedQueryResult_returnsIssueWhenBelowThreshold() throws Exception {
        var requestUri = PATH_API + PATH_QUERY + "/1" +  WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
        doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("FEASIBILITY_TEST_USER"));
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createTestDetailedObfuscatedQueryResultWithTooFewResults(thresholdSitesResult))
                .when(queryHandlerService).getQueryResult(any(Long.class), any(QueryHandlerService.ResultDetail.class));


        mockMvc.perform(get(requestUri).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultLines").doesNotExist())
                .andExpect(jsonPath("$.issues").exists())
                .andExpect(jsonPath("$.issues").isArray())
                .andExpect(jsonPath("$.issues[0].code").value("FEAS-" + FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SITES.code()));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetDetailedObfuscatedResult_failsOnWrongAuthorWith403() throws Exception {
        doReturn("some-other-user").when(queryHandlerService).getAuthorId(any(Long.class));

        mockMvc.perform(get(URI.create("/api/v3/query/1" + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQueryContent_succeeds() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createValidStructuredQuery()).when(queryHandlerService).getQueryContent(any(Long.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/1" + WebSecurityConfig.PATH_CONTENT)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inclusionCriteria").exists())
                .andExpect(jsonPath("$.inclusionCriteria", not(empty())));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQueryContent_failsOnWrongAuthorWith403() throws Exception {
        doReturn("not-test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createValidStructuredQuery()).when(queryHandlerService).getQueryContent(any(Long.class));

        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/1" + WebSecurityConfig.PATH_CONTENT)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetDetailedObfuscatedResultRateLimit_succeeds() throws Exception  {
        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/detailed-obfuscated-result-rate-limit")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").exists())
                .andExpect(jsonPath("$.remaining").exists());
    }

    @Test
    public void testGetDetailedObfuscatedResultRateLimit_failsOnNotLoggedIn() throws Exception  {
        mockMvc.perform(get(URI.create(PATH_API + PATH_QUERY + "/detailed-obfuscated-result-rate-limit")).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @NotNull
    private static StructuredQuery createValidStructuredQuery() {
        var termCode = TermCode.builder()
                .code("LL2191-6")
                .system("http://loinc.org")
                .display("Geschlecht")
                .build();
        var inclusionCriterion = Criterion.builder()
                .termCodes(List.of(termCode))
                .attributeFilters(List.of())
                .build();
        return StructuredQuery.builder()
                .version(URI.create("http://to_be_decided.com/draft-2/schema#"))
                .inclusionCriteria(List.of(List.of(inclusionCriterion)))
                .exclusionCriteria(List.of())
                .display("foo")
                .build();
    }

    @NotNull
    private static de.numcodex.feasibility_gui_backend.query.persistence.Query createValidQuery(long id) {
        var query = new de.numcodex.feasibility_gui_backend.query.persistence.Query();
        query.setId(id);
        query.setCreatedAt(new Timestamp(new java.util.Date().getTime()));
        query.setCreatedBy("someone");
        query.setQueryContent(createValidQueryContent(id));
        return query;
    }

    @NotNull
    private static de.numcodex.feasibility_gui_backend.query.persistence.QueryContent createValidQueryContent(long id) {
        var queryContent = new de.numcodex.feasibility_gui_backend.query.persistence.QueryContent();
        queryContent.setId(id);
        queryContent.setQueryContent(createValidStructuredQuery().toString());
        queryContent.setHash("abc");
        return queryContent;
    }

    @NotNull
    private static QueryListEntry createValidQueryListEntry(long id, boolean skipValidation) {
        return QueryListEntry.builder()
                .id(id)
                .label("abc")
                .createdAt(new Timestamp(new java.util.Date().getTime()))
                .invalidTerms(skipValidation ? List.of() : List.of(createTermCode()))
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
    private static Query createValidApiQuery(long id) {
        return Query.builder()
                .id(id)
                .content(createValidStructuredQuery())
                .label("test")
                .comment("test")
                .invalidTerms(List.of())
                .build();
    }

    @NotNull
    private static QueryResult createTestQueryResult(QueryHandlerService.ResultDetail resultDetail) {
        List<QueryResultLine> queryResultLines;
        long totalNumberOfPatients;

        if (resultDetail == QueryHandlerService.ResultDetail.SUMMARY) {
            queryResultLines = List.of();
            totalNumberOfPatients = 999L;
        } else {
            var resultLines = List.of(
                    ResultLine.builder()
                            .siteName("A")
                            .type(SUCCESS)
                            .result(123L)
                            .build(),
                    ResultLine.builder()
                            .siteName("B")
                            .type(SUCCESS)
                            .result(456L)
                            .build(),
                    ResultLine.builder()
                            .siteName("C")
                            .type(SUCCESS)
                            .result(789L)
                            .build()
            );
            queryResultLines = resultLines.stream()
                    .map(ssr -> QueryResultLine.builder()
                            .siteName(resultDetail == QueryHandlerService.ResultDetail.DETAILED_OBFUSCATED ? "foobar" + ssr.siteName()
                                    : ssr.siteName())
                            .numberOfPatients(ssr.result())
                            .build())
                    .toList();

            totalNumberOfPatients = queryResultLines.stream().map(QueryResultLine::numberOfPatients).reduce(0L, Long::sum);
        }

        return QueryResult.builder()
                .queryId(1L)
                .totalNumberOfPatients(totalNumberOfPatients)
                .resultLines(queryResultLines)
                .build();
    }

    @NotNull
    private static QueryResult createTestDetailedObfuscatedQueryResultWithTooFewResults(long threshold) {
        List<QueryResultLine> queryResultLines;


        var resultLines = List.of(
                ResultLine.builder()
                        .siteName("A")
                        .type(SUCCESS)
                        .result(threshold - 1)
                        .build(),
                ResultLine.builder()
                        .siteName("B")
                        .type(SUCCESS)
                        .result(threshold - 2)
                        .build(),
                ResultLine.builder()
                        .siteName("C")
                        .type(SUCCESS)
                        .result(threshold - 1)
                        .build()
        );
        queryResultLines = resultLines.stream()
                .map(ssr -> QueryResultLine.builder()
                        .siteName("foobar" + ssr.siteName())
                        .numberOfPatients(ssr.result())
                        .build())
                .toList();

        return QueryResult.builder()
                .queryId(1L)
                .totalNumberOfPatients(queryResultLines.stream().map(QueryResultLine::numberOfPatients).reduce(0L, Long::sum))
                .resultLines(queryResultLines)
                .build();
    }
}
