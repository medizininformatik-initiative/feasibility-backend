package de.numcodex.feasibility_gui_backend.query;

import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v1/query-handler")
@RestController
@CrossOrigin
@Slf4j
public class QueryHandlerRestController {

  private final QueryHandlerService queryHandlerService;
  private final String apiBaseUrl;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping("run-query")
  public Response runQuery(
      @RequestBody StructuredQuery query, @Context HttpServletRequest httpServletRequest) {

    Long queryId;
    try {
      queryId = queryHandlerService.runQuery(query);
    } catch (QueryDispatchException e) {
      log.error("Error while running query", e);
      return Response.status(INTERNAL_SERVER_ERROR).build();
    }

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
            ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
            : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uri = uriBuilder.replacePath("")
            .pathSegment("api", "v1", "query-handler", "result", String.valueOf(queryId))
            .build()
            .toUri();
    return Response.created(uri).build();
  }

  @GetMapping(path = "/result/{id}")
  public QueryResult getQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }
}
