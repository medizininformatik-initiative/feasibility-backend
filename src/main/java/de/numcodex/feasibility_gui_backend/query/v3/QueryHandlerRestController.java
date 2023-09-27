package de.numcodex.feasibility_gui_backend.query.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.config.WebSecurityConfig;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.api.Query;
import de.numcodex.feasibility_gui_backend.query.api.QueryListEntry;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultRateLimit;
import de.numcodex.feasibility_gui_backend.query.api.SavedQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.status.FeasibilityIssue;
import de.numcodex.feasibility_gui_backend.query.api.status.FeasibilityIssues;
import de.numcodex.feasibility_gui_backend.query.api.status.SavedQuerySlots;
import de.numcodex.feasibility_gui_backend.query.persistence.UserBlacklist;
import de.numcodex.feasibility_gui_backend.query.persistence.UserBlacklistRepository;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.AuthenticationHelper;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.InvalidAuthenticationException;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingService;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v3/query")
@RestController("QueryHandlerRestController-v3")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = {HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER})
public class QueryHandlerRestController {

  public static final String HEADER_X_DETAILED_OBFUSCATED_RESULT_WAS_EMPTY = "X-Detailed-Obfuscated-Result-Was-Empty";
  private final QueryHandlerService queryHandlerService;
  private final TermCodeValidation termCodeValidation;
  private final RateLimitingService rateLimitingService;
  private final UserBlacklistRepository userBlacklistRepository;
  private final AuthenticationHelper authenticationHelper;
  private final String apiBaseUrl;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;
  @Value("${app.keycloakPowerRole}")
  private String keycloakPowerRole;

  @Value("${app.privacy.quota.soft.create.amount}")
  private int quotaSoftCreateAmount;

  @Value("${app.privacy.quota.soft.create.intervalminutes}")
  private int quotaSoftCreateIntervalMinutes;

  @Value("${app.privacy.quota.hard.create.amount}")
  private int quotaHardCreateAmount;

  @Value("${app.privacy.quota.hard.create.intervalminutes}")
  private int quotaHardCreateIntervalMinutes;

  @Value("${app.privacy.threshold.sites}")
  private int privacyThresholdSites;

  @Value("${app.privacy.threshold.results}")
  private int privacyThresholdResults;

  @Value("${app.privacy.threshold.sitesResult}")
  private int privacyThresholdSitesResult;

  @Value("${app.maxSavedQueriesPerUser}")
  private int maxSavedQueriesPerUser;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      RateLimitingService rateLimitingService,
      TermCodeValidation termCodeValidation,
      UserBlacklistRepository userBlacklistRepository,
      AuthenticationHelper authenticationHelper,
      @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.rateLimitingService = rateLimitingService;
    this.termCodeValidation = termCodeValidation;
    this.userBlacklistRepository = userBlacklistRepository;
    this.authenticationHelper = authenticationHelper;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping
  public Mono<ResponseEntity<Object>> runQuery(
      @Valid @RequestBody StructuredQuery query,
      @Context HttpServletRequest request,
      Authentication authentication)
      throws InvalidAuthenticationException {
    String userId = authentication.getName();
    Optional<UserBlacklist> userBlacklistEntry = userBlacklistRepository.findByUserId(
        userId);
    boolean isPowerUser = authenticationHelper.hasAuthority(authentication,
        keycloakPowerRole);

    if (!isPowerUser && userBlacklistEntry.isPresent()) {
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.USER_BLACKLISTED_NOT_POWER_USER))
              .build();
      return Mono.just(
          new ResponseEntity<>(issues,
              HttpStatus.FORBIDDEN));
    }

    Long amountOfQueriesByUserAndHardInterval = queryHandlerService.getAmountOfQueriesByUserAndInterval(
        userId, quotaHardCreateIntervalMinutes);
    if (!isPowerUser && (quotaHardCreateAmount
        <= amountOfQueriesByUserAndHardInterval)) {
      log.info(
          "User {} exceeded hard limit and is not a power user. Blacklisting...",
          userId);
      UserBlacklist userBlacklist = new UserBlacklist();
      userBlacklist.setUserId(userId);
      userBlacklistRepository.save(userBlacklist);

      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.USER_BLACKLISTED_NOT_POWER_USER))
              .build();
      return Mono.just(
          new ResponseEntity<>(issues,
              HttpStatus.FORBIDDEN));
    }
    Long amountOfQueriesByUserAndSoftInterval = queryHandlerService.getAmountOfQueriesByUserAndInterval(
        userId, quotaSoftCreateIntervalMinutes);
    if (quotaSoftCreateAmount <= amountOfQueriesByUserAndSoftInterval) {
      Long retryAfter = queryHandlerService.getRetryAfterTime(userId,
          quotaSoftCreateAmount - 1, quotaSoftCreateIntervalMinutes);
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.add(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.QUOTA_EXCEEDED))
              .build();
      return Mono.just(
          new ResponseEntity<>(issues, httpHeaders,
              HttpStatus.TOO_MANY_REQUESTS));
    }
    // Note: this is using a ResponseEntity instead of a ServerResponse since this is a
    //       @Controller annotated class. This can be adjusted as soon as we switch to the new
    //       functional web framework (if ever).
    return queryHandlerService.runQuery(query, userId)
        .map(queryId -> buildResultLocationUri(request, queryId))
        .map(resultLocation -> ResponseEntity.created(resultLocation).build())
        .onErrorResume(e -> {
          log.error("running a query for '%s' failed".formatted(userId), e);
          return Mono.just(ResponseEntity.internalServerError()
              .body(e.getMessage()));
        });
  }

  private URI buildResultLocationUri(HttpServletRequest httpServletRequest,
      Long queryId) {
    UriComponentsBuilder uriBuilder =
        (apiBaseUrl != null && !apiBaseUrl.isEmpty())
            ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
            : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    return uriBuilder.replacePath("")
        .pathSegment("api", "v3", "query", String.valueOf(queryId))
        .build()
        .toUri();
  }

  @GetMapping("")
  public List<QueryListEntry> getQueryList(
      @RequestParam(name = "filter", required = false) String filter,
      Principal principal) {
    var userId = principal.getName();
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList = queryHandlerService.getQueryListForAuthor(userId, savedOnly);
    return queryHandlerService.convertQueriesToQueryListEntries(queryList);
  }

  @PostMapping("/{id}/saved")
  public ResponseEntity<Object> saveQuery(@PathVariable("id") Long queryId,
      @RequestBody SavedQuery savedQuery, Principal principal) {

    String authorId;
    try {
      authorId = queryHandlerService.getAuthorId(queryId);
    } catch (QueryNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    if (!authorId.equalsIgnoreCase(principal.getName())) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    Long amountOfSavedQueriesByUser = queryHandlerService.getAmountOfSavedQueriesByUser(authorId);
    if (amountOfSavedQueriesByUser >= maxSavedQueriesPerUser) {
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.SAVED_QUERY_STORAGE_FULL))
              .build();
      return new ResponseEntity<>(issues, HttpStatus.FORBIDDEN);
    }

    try {
      queryHandlerService.saveQuery(queryId, authorId, savedQuery);
      amountOfSavedQueriesByUser++;
      var savedQuerySlots = SavedQuerySlots.builder()
              .used(amountOfSavedQueriesByUser)
              .total(maxSavedQueriesPerUser)
              .build();
      return new ResponseEntity<>(savedQuerySlots, HttpStatus.OK);
    } catch (DataIntegrityViolationException e) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
  }

  @GetMapping("/saved-query-slots")
  public ResponseEntity<Object> getSavedQuerySlots(Principal principal) {
    return new ResponseEntity<>(getSavedQuerySlotsJson(principal), HttpStatus.OK);
  }

  @DeleteMapping("/{id}/saved")
  public ResponseEntity<Object> deleteSavedQuery(@PathVariable("id") Long queryId, Principal principal) {

    String authorId;
    try {
      authorId = queryHandlerService.getAuthorId(queryId);
    } catch (QueryNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    if (!authorId.equalsIgnoreCase(principal.getName())) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    try {
      queryHandlerService.deleteSavedQuery(queryId);
      return new ResponseEntity<>(getSavedQuerySlotsJson(principal), HttpStatus.OK);
    } catch (QueryNotFoundException e) {
      return new ResponseEntity<>(getSavedQuerySlotsJson(principal), HttpStatus.NOT_FOUND);
    }
  }

  private SavedQuerySlots getSavedQuerySlotsJson(Principal principal) {
    Long amountOfSavedQueriesByUser = queryHandlerService.getAmountOfSavedQueriesByUser(principal.getName());

    var savedQuerySlots = SavedQuerySlots.builder()
            .used(amountOfSavedQueriesByUser)
            .total(maxSavedQueriesPerUser)
            .build();
    return savedQuerySlots;
  }

  @GetMapping("/by-user/{id}")
  public List<QueryListEntry> getQueryListForUser(
      @PathVariable("id") String userId,
      @RequestParam(name = "filter", required = false) String filter) {
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList =  queryHandlerService.getQueryListForAuthor(userId, savedOnly);
    return queryHandlerService.convertQueriesToQueryListEntries(queryList);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> getQuery(@PathVariable("id") Long queryId,
      Authentication authentication) throws JsonProcessingException {
    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var query = queryHandlerService.getQuery(queryId);
    List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(query.content());
    var queryWithInvalidTerms = Query.builder()
            .id(query.id())
            .content(query.content())
            .label(query.label())
            .comment(query.comment())
            .invalidTerms(invalidTermCodes)
            .build();
    return new ResponseEntity<>(queryWithInvalidTerms, HttpStatus.OK);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_DETAILED_RESULT)
  public QueryResult getDetailedQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId, ResultDetail.DETAILED);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT)
  public ResponseEntity<Object> getDetailedObfuscatedQueryResult(@PathVariable("id") Long queryId,
   Authentication authentication) {
    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    QueryResult queryResult = queryHandlerService.getQueryResult(queryId,
        ResultDetail.DETAILED_OBFUSCATED);

    if (queryResult.totalNumberOfPatients() < privacyThresholdResults) {
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SIZE))
              .build();
      return new ResponseEntity<>(issues,
          HttpStatus.OK);
    }
    HttpHeaders headers = new HttpHeaders();
    if (queryResult.resultLines().stream().filter(result -> result.numberOfPatients() > privacyThresholdSitesResult).count() < privacyThresholdSites) {
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SITES))
              .build();
      return new ResponseEntity<>(
              issues,
              HttpStatus.OK);
    }
    if (queryResult.resultLines().isEmpty()) {
      headers.add(HEADER_X_DETAILED_OBFUSCATED_RESULT_WAS_EMPTY, "true");
    }
    return new ResponseEntity<>(queryResult, headers, HttpStatus.OK);
  }

  @GetMapping("/detailed-obfuscated-result-rate-limit")
  public ResponseEntity<Object> getDetailedObfuscatedResultRateLimit(
      Principal principal) {
    var userId = principal.getName();
    var bucket = this.rateLimitingService.resolveViewDetailedObfuscatedBucket(
        userId);

    QueryResultRateLimit resultRateLimit = QueryResultRateLimit.builder()
        .limit(this.rateLimitingService.getAmountDetailedObfuscated())
        .remaining(bucket.getAvailableTokens())
        .build();

    return new ResponseEntity<>(resultRateLimit, HttpStatus.OK);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_SUMMARY_RESULT)
  public ResponseEntity<Object> getSummaryQueryResult(
      @PathVariable("id") Long queryId,
      Authentication authentication) {
    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var queryResult = queryHandlerService.getQueryResult(queryId,
        ResultDetail.SUMMARY);

    if (queryResult.totalNumberOfPatients() < privacyThresholdResults) {
      var issues = FeasibilityIssues.builder()
              .issues(List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SIZE))
              .build();
      return new ResponseEntity<>(
          issues,
          HttpStatus.OK);
    }
    return new ResponseEntity<>(queryResult, HttpStatus.OK);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_CONTENT)
  public ResponseEntity<Object> getQueryContent(
      @PathVariable("id") Long queryId,
      Authentication authentication)
      throws JsonProcessingException {

    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var queryContent = queryHandlerService.getQueryContent(queryId);
    return new ResponseEntity<>(queryContent, HttpStatus.OK);
  }

  @PostMapping("/validate")
  public ResponseEntity<Object> validateStructuredQuery(
      @Valid @RequestBody StructuredQuery query) {
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private boolean hasAccess(Long queryId, Authentication authentication) {
    Set<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

    try {
      return (roles.contains(keycloakAdminRole)
          || queryHandlerService.getAuthorId(queryId)
          .equalsIgnoreCase(authentication.getName()));
    } catch (QueryNotFoundException e) {
      return false;
    }
  }
}
