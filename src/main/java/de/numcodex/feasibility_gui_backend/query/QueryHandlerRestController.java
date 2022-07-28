package de.numcodex.feasibility_gui_backend.query;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.StoredQuery;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.conversion.StoredQueryConverter;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import java.security.Principal;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v1/query-handler")
@RestController
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

  @PostMapping("run-query")
  public ResponseEntity<Object> runQuery(
      @Valid @RequestBody StructuredQuery query, @Context HttpServletRequest httpServletRequest) {

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

  @PostMapping(path = "/stored-query")
  public ResponseEntity<Object> storeQuery(@Valid @RequestBody StoredQuery query,
      @Context HttpServletRequest httpServletRequest,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    query.setCreatedBy(authorId);

    Long queryId;
    try {
      queryId = queryHandlerService.storeQuery(query);
    } catch (JsonProcessingException e) {
      log.error("Error while storing query", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataIntegrityViolationException e) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
        ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
        : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uriString = uriBuilder.replacePath("")
        .pathSegment("api", "v1", "query-handler", "stored-query", String.valueOf(queryId))
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
  }

  @GetMapping(path = "/stored-query/{queryId}")
  public ResponseEntity<Object> getStoredQuery(@PathVariable(value = "queryId") Long queryId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    try {
      var query = queryHandlerService.getQuery(queryId, authorId);
      StoredQuery storedQuery = StoredQueryConverter.convertPersistenceToApi(query);
      List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(storedQuery);
      storedQuery.setInvalidTerms(invalidTermCodes);
      return new ResponseEntity<>(storedQuery, HttpStatus.OK);
    } catch (JsonProcessingException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
  }

  @GetMapping(path = "/stored-query")
  public ResponseEntity<Object> getStoredQueryList(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    var queries = queryHandlerService.getQueriesForAuthor(authorId);
    var ret = new ArrayList<StoredQuery>();
    queries.forEach(q -> {
      try {
        StoredQuery convertedQuery = StoredQueryConverter.convertPersistenceToApi(q);
        convertedQuery.setStructuredQuery(null);
        ret.add(convertedQuery);
      } catch (JsonProcessingException e) {
        log.error("Error converting query");
      }
    });
    return new ResponseEntity<>(ret, HttpStatus.OK);
  }

  @GetMapping(path = "/stored-query/validate")
  public ResponseEntity<Object> validateStoredQueryList(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    var queries = queryHandlerService.getQueriesForAuthor(authorId);
    var ret = new ArrayList<StoredQuery>();
    queries.forEach(q -> {
      try {
        StoredQuery convertedQuery = StoredQueryConverter.convertPersistenceToApi(q);
        List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(convertedQuery);
        convertedQuery.setIsValid(invalidTermCodes.isEmpty());
        convertedQuery.setStructuredQuery(null);
        ret.add(convertedQuery);
      } catch (JsonProcessingException e) {
        log.error("Error converting query");
      }
    });
    return new ResponseEntity<>(ret, HttpStatus.OK);
  }

  /**
   * Read the subject id from a Authorization header with a JWT.
   */
  private String getUserIdFromAuthorizationHeader(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isEmpty()) {
      return null;
    }
    try {
      JWT jwt = new JWT();
      String accessTokenString = authorizationHeader
          .substring(authorizationHeader.indexOf(" ") + 1);
      DecodedJWT accessToken = jwt.decodeJwt(accessTokenString);
      return accessToken.getClaim("sub").asString();
    } catch (NullPointerException npe) {
      log.error("Nullpointer exception caught when trying to get user id from access token");
      return null;
    } catch (JWTDecodeException e) {
      log.error("Could not decode access token. Auth Header: " + authorizationHeader);
      return null;
    }
  }
}
