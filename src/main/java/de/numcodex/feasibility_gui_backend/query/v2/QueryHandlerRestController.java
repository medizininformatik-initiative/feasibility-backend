package de.numcodex.feasibility_gui_backend.query.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.config.WebSecurityConfig;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.api.QueryListEntry;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultRateLimit;
import de.numcodex.feasibility_gui_backend.query.api.SavedQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.status.FeasibilityIssue;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v2/query")
@RestController("QueryHandlerRestController-v2")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = {"Location"})
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
      return Mono.just(
          new ResponseEntity<>(FeasibilityIssue.USER_BLACKLISTED_NOT_POWER_USER,
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
      return Mono.just(
          new ResponseEntity<>(FeasibilityIssue.USER_BLACKLISTED_NOT_POWER_USER,
              HttpStatus.FORBIDDEN));
    }
    Long amountOfQueriesByUserAndSoftInterval = queryHandlerService.getAmountOfQueriesByUserAndInterval(
        userId, quotaSoftCreateIntervalMinutes);
    if (quotaSoftCreateAmount <= amountOfQueriesByUserAndSoftInterval) {
      Long retryAfter = queryHandlerService.getRetryAfterTime(userId,
          quotaSoftCreateAmount - 1, quotaSoftCreateIntervalMinutes);
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.add(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
      return Mono.just(
          new ResponseEntity<>(FeasibilityIssue.QUOTA_EXCEEDED, httpHeaders,
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
        .pathSegment("api", "v2", "query", String.valueOf(queryId))
        .build()
        .toUri();
  }

  @GetMapping("")
  public List<QueryListEntry> getQueryList(
      @RequestParam(name = "filter", required = false) String filter,
      Principal principal) {
    var userId = principal.getName();
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList = queryHandlerService.getQueryListForAuthor(userId,
        savedOnly);
    var queries = queryHandlerService.convertQueriesToQueryListEntries(
        queryList);
    return queries;
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
    } else {
      try {
        queryHandlerService.saveQuery(queryId, savedQuery);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      } catch (DataIntegrityViolationException e) {
        return new ResponseEntity<>(HttpStatus.CONFLICT);
      }
    }
  }

  @GetMapping("/by-user/{id}")
  public List<QueryListEntry> getQueryListForUser(
      @PathVariable("id") String userId,
      @RequestParam(name = "filter", required = false) String filter) {
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList = queryHandlerService.getQueryListForAuthor(userId,
        savedOnly);
    var queries = queryHandlerService.convertQueriesToQueryListEntries(
        queryList);
    return queries;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> getQuery(@PathVariable("id") Long queryId,
      Authentication authentication) throws JsonProcessingException {
    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var query = queryHandlerService.getQuery(queryId);
    List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(
        query.getContent());
    query.setInvalidTerms(invalidTermCodes);
    return new ResponseEntity<>(query, HttpStatus.OK);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_DETAILED_RESULT)
  public QueryResult getDetailedQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId, ResultDetail.DETAILED);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_DETAILED_OBFUSCATED_RESULT)
  public ResponseEntity<Object> getDetailedObfuscatedQueryResult(
      @PathVariable("id") Long queryId) {
    QueryResult queryResult = queryHandlerService.getQueryResult(queryId,
        ResultDetail.DETAILED_OBFUSCATED);

    if (queryResult.getTotalNumberOfPatients() < privacyThresholdResults) {
      return new ResponseEntity<>(
          List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SIZE),
          HttpStatus.OK);
    }
    HttpHeaders headers = new HttpHeaders();
    if (queryResult.getResultLines().size() < privacyThresholdSites) {
      return new ResponseEntity<>(
          List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SITES),
          HttpStatus.OK);
    }
    if (queryResult.getResultLines().isEmpty()) {
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
      return new ResponseEntity<>(
          List.of(FeasibilityIssue.USER_INCORRECT_ACCESS_RIGHTS),
          HttpStatus.FORBIDDEN);
    }
    var queryResult = queryHandlerService.getQueryResult(queryId,
        ResultDetail.SUMMARY);

    if (queryResult.getTotalNumberOfPatients() < privacyThresholdResults) {
      return new ResponseEntity<>(
          List.of(FeasibilityIssue.PRIVACY_RESTRICTION_RESULT_SIZE),
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
