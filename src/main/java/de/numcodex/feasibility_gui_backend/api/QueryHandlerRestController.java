package de.numcodex.feasibility_gui_backend.api;

import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.service.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
  public Response runQuery(@RequestBody StructuredQuery query, @Context HttpServletRequest httpServletRequest) {
    String id;
    try {
      id = queryHandlerService.runQuery(query);
    } catch (UnsupportedMediaTypeException | QueryNotFoundException | IOException e) {
      // TODO: Find correct Http error handling
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    try {
      URI uri = createUri(httpServletRequest, id);
      return Response.created(uri).build();
    } catch (URISyntaxException e) {
      // TODO: Find correct Http error handling
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  private URI createUri(HttpServletRequest httpServletRequest, String id) throws URISyntaxException {
    var scheme = httpServletRequest.getScheme();
    var serverNmame = httpServletRequest.getServerName();
    var port = httpServletRequest.getServerPort();

    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(scheme);
    uriBuilder.setHost(serverNmame);
    uriBuilder.setPort(port);

    return uriBuilder.setPathSegments("api", "v1", "query-handler", "result", id).build();
  }

  @GetMapping(path = "/result/{id}")
  public QueryResult getQueryResult(@PathVariable("id") String queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }
}
