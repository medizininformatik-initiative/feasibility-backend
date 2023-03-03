package de.numcodex.feasibility_gui_backend.query.v1;

import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService.ResultDetail;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;
import java.net.URI;
import java.security.Principal;

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

  @Value("${app.security.nqueries.amount}")
  private int nQueriesAmount;

  @Value("${app.security.nqueries.perminutes}")
  private int nQueriesPerMinute;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping(value = "run-query")
  @Deprecated
  public Mono<ResponseEntity<Object>> runQuery(@Valid @RequestBody StructuredQuery query,
      @Context HttpServletRequest request,
      Principal principal) {

    Long amountOfQueriesByUserAndInterval = queryHandlerService.getAmountOfQueriesByUserAndInterval(
        principal.getName(), nQueriesPerMinute);
    log.error("amount: " + amountOfQueriesByUserAndInterval);
    if (nQueriesAmount <= amountOfQueriesByUserAndInterval) {
      Long retryAfter = queryHandlerService.getRetryAfterTime(principal.getName(),
          nQueriesAmount - 1, nQueriesPerMinute);
      log.error("retry after: " + retryAfter);
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
            .pathSegment("api", "v1", "query-handler", "result", String.valueOf(queryId))
            .build()
            .toUri();
  }

  @GetMapping(path = "/result/{id}")
  @Deprecated
  public ResponseEntity<Object> getQueryResult(@PathVariable("id") Long queryId,
      Authentication authentication) {

    if (queryHandlerService.getAuthorId(queryId).equalsIgnoreCase(authentication.getName())) {
      return new ResponseEntity<>(
          queryHandlerService.getQueryResult(queryId, ResultDetail.DETAILED_OBFUSCATED_SITENAMES),
          HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
  }
}
