package de.numcodex.feasibility_gui_backend.api;

import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.service.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilderException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    Long id;
    try {
      id = queryHandlerService.runQuery(query);
    } catch (UnsupportedMediaTypeException e) {
      log.error("Unsupported Media Type submitted", e);
      return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
    } catch (QueryNotFoundException e) {
      log.error("Query not found", e);
      return Response.status(Status.NOT_FOUND).build();
    } catch (IOException | QueryBuilderException e) {
      // TODO: Find correct Http error handling
      log.error("problem running query", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
            ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
            : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uri = uriBuilder.replacePath("")
            .pathSegment("api", "v1", "query-handler", "result", String.valueOf(id))
            .build()
            .toUri();
    return Response.created(uri).build();
  }

  @GetMapping(path = "/result/{id}")
  public QueryResult getQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }
}
