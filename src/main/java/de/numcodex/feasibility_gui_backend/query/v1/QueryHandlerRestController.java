package de.numcodex.feasibility_gui_backend.query.v1;

import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v1/query-handler")
@RestController("QueryHandlerRestController-v1")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class QueryHandlerRestController {

  private final QueryHandlerService queryHandlerService;
  private final String apiBaseUrl;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping("run-query")
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
        .pathSegment("api", "v1", "query-handler", "result", String.valueOf(queryId))
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
  }

  @GetMapping(path = "/result/{id}")
  public QueryResult getQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }
}
