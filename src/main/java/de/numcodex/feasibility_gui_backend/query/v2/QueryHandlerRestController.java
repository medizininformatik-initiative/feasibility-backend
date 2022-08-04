package de.numcodex.feasibility_gui_backend.query.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.Query;
import de.numcodex.feasibility_gui_backend.query.api.QueryListEntry;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.SavedQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import java.security.Principal;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  @Value("${keycloak.enabled}")
  private boolean keycloakEnabled;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      TermCodeValidation termCodeValidation, @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.termCodeValidation = termCodeValidation;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping("")
  @PreAuthorize("hasRole(@environment.getProperty('app.keycloakAllowedRole'))")
  public ResponseEntity<Object> runQuery(@Valid @RequestBody StructuredQuery query,
      @Context HttpServletRequest httpServletRequest, Principal principal) {

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
        .pathSegment("api", "v2", "query", String.valueOf(queryId), "result")
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
  }

  @GetMapping("")
  @PreAuthorize("hasRole(@environment.getProperty('app.keycloakAllowedRole'))")
  public List<QueryListEntry> getQueryList(@RequestParam(name = "filter", required = false) String filter, Principal principal) {
    var userId = principal.getName();
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList = queryHandlerService.getQueryListForAuthor(userId, savedOnly);
    var queries = queryHandlerService.convertQueriesToQueryListEntries(queryList);
    return queries;
  }

  @PostMapping("/{id}/saved")
  @PreAuthorize("hasRole(@environment.getProperty('app.keycloakAllowedRole'))")
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
  @PreAuthorize("hasRole(@environment.getProperty('app.keycloakAdminRole'))")
  public List<QueryListEntry> getQueryListForUser(@PathVariable("id") String userId, @RequestParam("filter") String filter) {
    var savedOnly = (filter != null && filter.equalsIgnoreCase("saved"));
    var queryList =  queryHandlerService.getQueryListForAuthor(userId, savedOnly);
    var queries = queryHandlerService.convertQueriesToQueryListEntries(queryList);
    return queries;
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole(@environment.getProperty('app.keycloakAllowedRole'), @environment.getProperty('app.keycloakAdminRole'))")
  public Query getQuery(@PathVariable("id") Long queryId) throws JsonProcessingException {
    return queryHandlerService.getQuery(queryId);
  }

  @GetMapping("/{id}/result/detailed")
  @PreAuthorize("hasRole(@environment.getProperty('app.keycloakAdminRole'))")
  public QueryResult getQueryResultObfuscated(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }

  @GetMapping("/{id}/result")
  @PreAuthorize("hasAnyRole(@environment.getProperty('app.keycloakAllowedRole'), @environment.getProperty('app.keycloakAdminRole'))")
  public QueryResult getQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }

  @GetMapping("/{id}/content")
  @PreAuthorize("hasAnyRole(@environment.getProperty('app.keycloakAllowedRole'), @environment.getProperty('app.keycloakAdminRole'))")
  public StructuredQuery getQueryContent(@PathVariable("id") Long queryId)
      throws JsonProcessingException {
    return queryHandlerService.getQueryContent(queryId);
  }
}
