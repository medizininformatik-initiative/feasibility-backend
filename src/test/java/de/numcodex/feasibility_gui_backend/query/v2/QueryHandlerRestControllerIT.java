package de.numcodex.feasibility_gui_backend.query.v2;
import de.numcodex.feasibility_gui_backend.config.WebSecurityConfig;
import de.numcodex.feasibility_gui_backend.query.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.api.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.validation.StructuredQueryValidatorSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.persistence.ResultType;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.*;
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

    @BeforeEach
    void initTest() throws Exception {
        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testRunQueryEndpoint_FailsOnInvalidStructuredQueryWith400() throws Exception {
        var testQuery = new StructuredQuery(null, null, null, null);

        mockMvc.perform(post(URI.create("/api/v2/query")).with(csrf())
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

        var mvcResult = mockMvc.perform(post(URI.create("/api/v2/query")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", "/api/v2/query/1"));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_FailsOnDownstreamServiceError() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        var dispatchError = new QueryDispatchException("something went wrong");

        doReturn(Mono.error(dispatchError)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));
        doReturn(List.of()).when(termCodeValidation).getInvalidTermCodes(any(StructuredQuery.class));

        var mvcResult = mockMvc.perform(post(URI.create("/api/v2/query")).with(csrf())
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

        var mvcResult = mockMvc.perform(post(URI.create("/api/v2/query")).with(csrf())
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

        mockMvc.perform(post(URI.create("/api/v2/query/validate")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER")
    public void testValidateQueryEndpoint_FailsOnInvalidStructuredQueryWith400() throws Exception {
        var testQuery = new StructuredQuery(null, null, null, null);

        mockMvc.perform(post(URI.create("/api/v2/query/validate")).with(csrf())
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

        var mvcResult = mockMvc.perform(post(URI.create("/api/v2/query")).with(csrf())
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

        var mvcResult = mockMvc.perform(post(URI.create("/api/v2/query")).with(csrf())
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

        var mvcResult = mockMvc.perform(post(URI.create("/api/v2/query")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isCreated())
            .andExpect(header().exists("location"))
            .andExpect(header().string("location", "/api/v2/query/1"));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQueryList_Succeeds() throws Exception {
        long queryId = 1;
        doReturn(List.of(createValidQuery(queryId))).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of(createValidQueryListEntry(queryId))).when(queryHandlerService).convertQueriesToQueryListEntries(anyList());

        mockMvc.perform(get(URI.create("/api/v2/query")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(queryId));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_ADMIN"}, username = "test")
    public void testGetQueryListForUser_SucceedsOnValidUser() throws Exception {
        long queryId = 1;
        String userId = "user1";
        doReturn(List.of(createValidQuery(queryId))).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of(createValidQueryListEntry(queryId))).when(queryHandlerService).convertQueriesToQueryListEntries(anyList());

        mockMvc.perform(get(URI.create("/api/v2/query/by-user/" + userId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(queryId));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_ADMIN"}, username = "test")
    public void testGetQueryListForUser_ReturnsEmptyOnUnknownUser() throws Exception {
        String userId = "user1";
        doReturn(List.of()).when(queryHandlerService).getQueryListForAuthor(any(String.class), any(Boolean.class));
        doReturn(List.of()).when(queryHandlerService).convertQueriesToQueryListEntries(anyList());

        mockMvc.perform(get(URI.create("/api/v2/query/by-user/" + userId)).with(csrf()))
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

        mockMvc.perform(get(URI.create("/api/v2/query/" + queryId)).with(csrf()))
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

        mockMvc.perform(get(URI.create("/api/v2/query/" + queryId)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_Succeeds() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));

        var savedQuery = new SavedQuery("foo", "bar");

        mockMvc.perform(post(URI.create("/api/v2/query/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_failsWith404OnAuthorForQueryNotFound() throws Exception {
        doThrow(QueryNotFoundException.class).when(queryHandlerService).getAuthorId(any(Long.class));

        var savedQuery = new SavedQuery("foo", "bar");

        mockMvc.perform(post(URI.create("/api/v2/query/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_failsWith403OnAuthorMismatch() throws Exception {
        doReturn("SomeOtherUser").when(queryHandlerService).getAuthorId(any(Long.class));

        var savedQuery = new SavedQuery("foo", "bar");

        mockMvc.perform(post(URI.create("/api/v2/query/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    void testSaveQuery_failsWith409OnExistingSavedQuery() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doThrow(DataIntegrityViolationException.class).when(queryHandlerService).saveQuery(any(Long.class), any(SavedQuery.class));

        var savedQuery = new SavedQuery("foo", "bar");

        mockMvc.perform(post(URI.create("/api/v2/query/1/saved")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(savedQuery)))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @EnumSource
    @WithMockUser(roles = {"FEASIBILITY_TEST_ADMIN"}, username = "test")
    public void testGetQueryResult_succeeds(QueryHandlerService.ResultDetail resultDetail) throws Exception {
        var requestUri = "/api/v2/query/1";
        doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("FEASIBILITY_TEST_ADMIN"));
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createTestQueryResult(resultDetail)).when(queryHandlerService).getQueryResult(any(Long.class), any(QueryHandlerService.ResultDetail.class));

        switch (resultDetail) {
            case SUMMARY -> {
                requestUri = requestUri + WebSecurityConfig.PATH_SUMMARY_RESULT;
            }
            case DETAILED_OBFUSCATED -> {
                requestUri = requestUri +  WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT;
            }
            case DETAILED -> {
                requestUri = requestUri +  WebSecurityConfig.PATH_DETAILED_RESULT;
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
    public void testGetQueryContent_succeeds() throws Exception {
        doReturn("test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createValidStructuredQuery()).when(queryHandlerService).getQueryContent(any(Long.class));

        mockMvc.perform(get(URI.create("/api/v2/query/1" + WebSecurityConfig.PATH_CONTENT)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inclusionCriteria").exists())
                .andExpect(jsonPath("$.inclusionCriteria", not(empty())));
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetQueryContent_failsOnWrongAuthorWith403() throws Exception {
        doReturn("not-test").when(queryHandlerService).getAuthorId(any(Long.class));
        doReturn(createValidStructuredQuery()).when(queryHandlerService).getQueryContent(any(Long.class));

        mockMvc.perform(get(URI.create("/api/v2/query/1" + WebSecurityConfig.PATH_CONTENT)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER"}, username = "test")
    public void testGetDetailedObfuscatedResultRateLimit_succeeds() throws Exception  {
        mockMvc.perform(get(URI.create("/api/v2/query/detailed-obfuscated-result-rate-limit")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").exists())
                .andExpect(jsonPath("$.remaining").exists());
    }

    @Test
    public void testGetDetailedObfuscatedResultRateLimit_failsOnNotLoggedIn() throws Exception  {
        mockMvc.perform(get(URI.create("/api/v2/query/detailed-obfuscated-result-rate-limit")).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @NotNull
    private static StructuredQuery createValidStructuredQuery() {
        var termCode = new TermCode("LL2191-6", "http://loinc.org", null, "Geschlecht");
        var inclusionCriterion = new Criterion(List.of(termCode), List.of(), null, null);
        return new StructuredQuery(URI.create("http://to_be_decided.com/draft-2/schema#"),
                List.of(List.of(inclusionCriterion)), List.of(), "foo");
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
    private static QueryListEntry createValidQueryListEntry(long id) {
        return new QueryListEntry(id, "abc", new Timestamp(new java.util.Date().getTime()));
    }

    @NotNull
    private static Query createValidApiQuery(long id) {
        return new Query(id, createValidStructuredQuery(), "test", "test", List.of());
    }

    @NotNull
    private static QueryResult createTestQueryResult(QueryHandlerService.ResultDetail resultDetail) {
        List<QueryResultLine> queryResultLines;

        if (resultDetail == QueryHandlerService.ResultDetail.SUMMARY) {
            queryResultLines = List.of();
        } else {
            var resultLines = List.of(
                    new ResultLine("A", ResultType.SUCCESS, 123L),
                    new ResultLine("B", ResultType.SUCCESS, 456L),
                    new ResultLine("C", ResultType.SUCCESS, 789L)
            );
            queryResultLines = resultLines.stream()
                    .map(ssr -> QueryResultLine.builder()
                            .siteName(resultDetail == QueryHandlerService.ResultDetail.DETAILED_OBFUSCATED ? "foobar" + ssr.siteName()
                                    : ssr.siteName())
                            .numberOfPatients(ssr.result())
                            .build())
                    .toList();
        }

        return QueryResult.builder()
                .queryId(1L)
                .totalNumberOfPatients(123L)
                .resultLines(queryResultLines)
                .build();
    }
}
