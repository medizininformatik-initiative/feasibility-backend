package de.numcodex.feasibility_gui_backend.query.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.QueryListEntry;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.SavedQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      TermCodeValidation termCodeValidation, @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.termCodeValidation = termCodeValidation;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping("")
  public ResponseEntity<Object> runQuery(@Valid @RequestBody StructuredQuery query,
      @Context HttpServletRequest httpServletRequest, Principal principal) {

    if (nQueriesAmount < queryHandlerService.getAmountOfQueriesByUserAndInterval(
        principal.getName(), nQueriesPerMinute)) {
      return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
    }

    Long queryId;
    try {
      queryId = queryHandlerService.runQuery(query, principal.getName());
    } catch (QueryDispatchException e) {
      log.error("Error while running query", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
        ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
        : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uriString = uriBuilder.replacePath("")
        .pathSegment("api", "v2", "query", String.valueOf(queryId))
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
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

  @GetMapping("/by-user/{id}")
  public List<QueryListEntry> getQueryListForUser(@PathVariable("id") String userId, @RequestParam(name = "filter", required = false) String filter) {
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList =  queryHandlerService.getQueryListForAuthor(userId, savedOnly);
    var queries = queryHandlerService.convertQueriesToQueryListEntries(queryList);
    return queries;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> getQuery(@PathVariable("id") Long queryId,
      KeycloakAuthenticationToken keycloakAuthenticationToken) throws JsonProcessingException {
    if (!hasAccess(queryId, keycloakAuthenticationToken)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var query = queryHandlerService.getQuery(queryId);
    List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(
        query.getContent());
    query.setInvalidTerms(invalidTermCodes);
    return new ResponseEntity<>(query, HttpStatus.OK);
  }

  @GetMapping("/{id}/result/detailed")
  public QueryResult getQueryResultDetailed(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId, false);
  }

  @GetMapping("/{id}/result")
  public ResponseEntity<Object> getQueryResult(@PathVariable("id") Long queryId,
      KeycloakAuthenticationToken keycloakAuthenticationToken) {
    if (!hasAccess(queryId, keycloakAuthenticationToken)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var queryResult = queryHandlerService.getQueryResult(queryId);
    return new ResponseEntity<>(queryResult, HttpStatus.OK);
  }

  @GetMapping("/{id}/content")
  public ResponseEntity<Object> getQueryContent(@PathVariable("id") Long queryId,
      KeycloakAuthenticationToken keycloakAuthenticationToken)
      throws JsonProcessingException {

    if (!hasAccess(queryId, keycloakAuthenticationToken)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    var queryContent = queryHandlerService.getQueryContent(queryId);
    return new ResponseEntity<>(queryContent, HttpStatus.OK);
  }

  private boolean hasAccess(Long queryId, KeycloakAuthenticationToken keycloakAuthenticationToken) {
    KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) keycloakAuthenticationToken.getPrincipal();
    RefreshableKeycloakSecurityContext keycloakSecurityContext =
        (RefreshableKeycloakSecurityContext) keycloakPrincipal.getKeycloakSecurityContext();
    Set<String> roles = keycloakSecurityContext.getToken().getRealmAccess().getRoles();

    return (roles.contains(keycloakAdminRole) || queryHandlerService.getAuthorId(queryId)
        .equalsIgnoreCase(keycloakPrincipal.getName()));
  }
}
