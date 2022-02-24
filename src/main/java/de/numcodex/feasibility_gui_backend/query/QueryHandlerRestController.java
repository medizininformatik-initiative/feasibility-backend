package de.numcodex.feasibility_gui_backend.query;

import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.validation.QueryValidator;
import java.io.FileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

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
  private final QueryValidator queryValidator;

  @Value("${app.enableQueryValidation}")
  private boolean queryValidationEnabled;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      @Value("${app.apiBaseUrl}") String apiBaseUrl, QueryValidator queryValidator) {
    this.queryHandlerService = queryHandlerService;
    this.apiBaseUrl = apiBaseUrl;
    this.queryValidator = queryValidator;
  }

  @PostMapping("run-query")
  public ResponseEntity<Object> runQuery(
      @RequestBody StructuredQuery query, @Context HttpServletRequest httpServletRequest) {

    if (queryValidationEnabled) {
      try {
        queryValidator.validate(query);
      } catch (ValidationException e) {
        log.warn("Query validation failed: {}", e.getMessage());
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
      } catch (FileNotFoundException e) {
        log.error("JSON query schema file could not be found or read");
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (JSONException e) {
        log.error("JSON query schema file malformed");
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    Long queryId;
    try {
      queryId = queryHandlerService.runQuery(query);
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
