package de.numcodex.feasibility_gui_backend.query.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.Query;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    if (principal == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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
        .pathSegment("api", "v2", "query", String.valueOf(queryId), "result", "obfuscated")
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
  }

  @GetMapping("")
  @PreAuthorize("hasRole(@environment.getProperty('app.keycloakAllowedRole'))")
  public Query getQueryList(@RequestParam("filter") String filter, Principal principal) {
//    return queryHandlerService.getQuery();
    // TODO
    return new Query();
  }

  @GetMapping("/by-user/{userId}")
  @PreAuthorize("hasRole(@environment.getProperty('app.keycloakAdminRole'))")
  public Query getQueryList(@PathVariable("userId") String userId, @RequestParam("filter") String filter, Principal principal) {
//    return queryHandlerService.getQuery();
    // TODO
    return new Query();
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
