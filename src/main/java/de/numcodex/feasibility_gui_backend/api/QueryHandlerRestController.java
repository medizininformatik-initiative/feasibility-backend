package de.numcodex.feasibility_gui_backend.api;

import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.service.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v1/query-handler")
@RestController
@CrossOrigin
public class QueryHandlerRestController {

  private final QueryHandlerService queryHandlerService;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService) {
    this.queryHandlerService = queryHandlerService;
  }

  @PostMapping("run-query")
  public Response runQuery(
      @RequestBody StructuredQuery query, @Context HttpServletRequest httpServletRequest) {
    String id;
    try {
      id = queryHandlerService.runQuery(query);
    } catch (UnsupportedMediaTypeException | QueryNotFoundException | IOException e) {
      // TODO: Find correct Http error handling
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    URI uri =
        ServletUriComponentsBuilder.fromRequestUri(httpServletRequest)
            .replacePath("")
            .pathSegment("api", "v1", "query-handler", "result", id)
            .build()
            .toUri();
    return Response.created(uri).build();
  }

  @GetMapping(path = "/result/{id}")
  public QueryResult getQueryResult(@PathVariable("id") String queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }
}
