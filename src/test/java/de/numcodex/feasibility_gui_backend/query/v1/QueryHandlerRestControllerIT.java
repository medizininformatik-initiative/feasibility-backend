package de.numcodex.feasibility_gui_backend.query.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.validation.StructuredQueryValidatorSpringConfig;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.persistence.UserBlacklist;
import de.numcodex.feasibility_gui_backend.query.persistence.UserBlacklistRepository;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.AuthenticationHelper;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import java.sql.Timestamp;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("query")
@ExtendWith(SpringExtension.class)
@Import({StructuredQueryValidatorSpringConfig.class})
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
        var testQuery = new StructuredQuery();

        mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_SucceedsOnValidStructuredQueryWith201() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));

        var mvcResult = mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", "/api/v1/query-handler/result/1"));
    }

    @Test
    @WithMockUser(roles = "FEASIBILITY_TEST_USER", username = "test")
    public void testRunQueryEndpoint_FailsOnDownstreamServiceError() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        var dispatchError = new QueryDispatchException("something went wrong");

        doReturn(Mono.error(dispatchError)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));

        var mvcResult = mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query")).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonUtil.writeValueAsString(testQuery)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
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

        var mvcResult = mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query")).with(csrf())
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

        var mvcResult = mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    @WithMockUser(roles = {"FEASIBILITY_TEST_USER", "FEASIBILITY_TEST_POWER"}, username = "test")
    public void testRunQueryEndpoint_SucceedsOnExceedingHardlimitAsPowerUserWith201() throws Exception {
        StructuredQuery testQuery = createValidStructuredQuery();

        doReturn(true).when(authenticationHelper).hasAuthority(any(Authentication.class), eq("FEASIBILITY_TEST_POWER"));
        doReturn((long)quotaHardCreateAmount).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaHardCreateIntervalMinutes));
        doReturn((long)(quotaSoftCreateAmount - 1)).when(queryHandlerService).getAmountOfQueriesByUserAndInterval(any(String.class), eq(quotaSoftCreateIntervalMinutes));
        doReturn(Mono.just(1L)).when(queryHandlerService).runQuery(any(StructuredQuery.class), eq("test"));

        var mvcResult = mockMvc.perform(post(URI.create("/api/v1/query-handler/run-query")).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(jsonUtil.writeValueAsString(testQuery)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isCreated())
            .andExpect(header().exists("location"))
            .andExpect(header().string("location", "/api/v1/query-handler/result/1"));
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
}
