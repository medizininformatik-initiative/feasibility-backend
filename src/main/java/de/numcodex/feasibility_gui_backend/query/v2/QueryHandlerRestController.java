package de.numcodex.feasibility_gui_backend.query.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.config.WebSecurityConfig;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.api.QueryListEntry;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.SavedQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Set;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v2/query")
@RestController("QueryHandlerRestController-v2")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class QueryHandlerRestController {

  private final QueryHandlerService queryHandlerService;
  private final TermCodeValidation termCodeValidation;
  private final String apiBaseUrl;

  @Value("${app.keycloakAdminRole}")
  private String keycloakAdminRole;

  @Value("${app.security.nqueries.amount}")
  private int nQueriesAmount;

  @Value("${app.security.nqueries.perminutes}")
  private int nQueriesPerMinute;

  @Value("${PRIVACY_THRESHOLD_SITES:3}")
  private int privacyThresholdSites;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      TermCodeValidation termCodeValidation, @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.termCodeValidation = termCodeValidation;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping
  public Mono<ResponseEntity<Object>> runQuery(@Valid @RequestBody StructuredQuery query,
                                               @Context HttpServletRequest request,
                                               Principal principal) {

    Long amountOfQueriesByUserAndInterval = queryHandlerService.getAmountOfQueriesByUserAndInterval(
        principal.getName(), nQueriesPerMinute);
    if (nQueriesAmount <= amountOfQueriesByUserAndInterval) {
      Long retryAfter = queryHandlerService.getRetryAfterTime(principal.getName(),
          nQueriesAmount - 1, nQueriesPerMinute);
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.add(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
      return Mono.just(new ResponseEntity<>(httpHeaders, HttpStatus.TOO_MANY_REQUESTS));
      //return new ResponseEntity<>(httpHeaders, HttpStatus.TOO_MANY_REQUESTS);
    }
    // Note: this is using a ResponseEntity instead of a ServerResponse since this is a
    //       @Controller annotated class. This can be adjusted as soon as we switch to the new
    //       functional web framework (if ever).
    return queryHandlerService.runQuery(query, principal.getName())
            .map(queryId -> buildResultLocationUri(request, queryId))
            .map(resultLocation -> ResponseEntity.created(resultLocation).build())
            .onErrorResume(e -> {
              log.error("running a query for '%s' failed".formatted(principal.getName()), e);
              return Mono.just(ResponseEntity.internalServerError()
                      .body(e.getMessage()));
            });
  }

  private URI buildResultLocationUri(HttpServletRequest httpServletRequest,
                                     Long queryId) {
      UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
              ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
              : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

      return uriBuilder.replacePath("")
              .pathSegment("api", "v2", "query", String.valueOf(queryId))
              .build()
              .toUri();
  }

  @GetMapping("")
  public List<QueryListEntry> getQueryList(@RequestParam(name = "filter", required = false) String filter, Principal principal) {
    var userId = principal.getName();
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList = queryHandlerService.getQueryListForAuthor(userId, savedOnly);
    var queries = queryHandlerService.convertQueriesToQueryListEntries(queryList);
    return queries;
  }

  @PostMapping("/{id}/saved")
  public ResponseEntity<Object> saveQuery(@PathVariable("id") Long queryId,
      @RequestBody SavedQuery savedQuery,  Principal principal) {

    String authorId = queryHandlerService.getAuthorId(queryId);
    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else if (!authorId.equalsIgnoreCase(principal.getName())) {
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

  @GetMapping( "/by-user/{id}")
  public List<QueryListEntry> getQueryListForUser(@PathVariable("id") String userId, @RequestParam(name = "filter", required = false) String filter) {
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList =  queryHandlerService.getQueryListForAuthor(userId, savedOnly);
    var queries = queryHandlerService.convertQueriesToQueryListEntries(queryList);
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
  public QueryResult getDetailedObfuscatedQueryResult(@PathVariable("id") Long queryId) {
    QueryResult queryResult = queryHandlerService.getQueryResult(queryId,
        ResultDetail.DETAILED_OBFUSCATED);
    if (queryResult.getResultLines().size() < privacyThresholdSites) {
      queryResult.setResultLines(List.of());
    }
    return queryResult;
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_SUMMARY_RESULT)
  public ResponseEntity<Object> getSummaryQueryResult(@PathVariable("id") Long queryId,
      Authentication authentication) {
    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var queryResult = queryHandlerService.getQueryResult(queryId, ResultDetail.SUMMARY);
    return new ResponseEntity<>(queryResult, HttpStatus.OK);
  }

  @GetMapping("/{id}" + WebSecurityConfig.PATH_CONTENT)
  public ResponseEntity<Object> getQueryContent(@PathVariable("id") Long queryId,
      Authentication authentication)
      throws JsonProcessingException {

    if (!hasAccess(queryId, authentication)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var queryContent = queryHandlerService.getQueryContent(queryId);
    return new ResponseEntity<>(queryContent, HttpStatus.OK);
  }

  private boolean hasAccess(Long queryId, Authentication authentication) {
    Set<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

    return (roles.contains(keycloakAdminRole) || queryHandlerService.getAuthorId(queryId)
        .equalsIgnoreCase(authentication.getName()));
  }
}
