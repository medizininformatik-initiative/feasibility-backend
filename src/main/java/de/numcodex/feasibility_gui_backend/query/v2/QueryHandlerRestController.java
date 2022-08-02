package de.numcodex.feasibility_gui_backend.query.v2;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateException;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("api/v2/query")
@RestController("QueryHandlerRestController-v2")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class QueryHandlerRestController {

  public static String KEYCLOAK_ALLOWED_ROLE;
  public static String FDPG_CLIENT_ID;

  private final QueryHandlerService queryHandlerService;
  private final TermCodeValidation termCodeValidation;
  private final String apiBaseUrl;


  @Value("${keycloak.enabled}")
  private boolean keycloakEnabled;

  @Value("${app.keycloakAllowedRole}")
  public void setKeycloakAllowedRole(String keycloakAllowedRole) {
    KEYCLOAK_ALLOWED_ROLE = keycloakAllowedRole;
  }

  @Value("${app.fdpgClientId}")
  public void setFdpgClientId(String fdpgClientId) {
    FDPG_CLIENT_ID = fdpgClientId;
  }

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      TermCodeValidation termCodeValidation, @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.termCodeValidation = termCodeValidation;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping("")
  @PreAuthorize("hasRole({T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).KEYCLOAK_ALLOWED_ROLE})")
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

  @GetMapping("/{id}/result/obfuscated")
  @PreAuthorize("hasRole({T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).KEYCLOAK_ALLOWED_ROLE}) "
      + "or principal.name == {T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).FDPG_CLIENT_ID}")
  public QueryResult getQueryResultObfuscated(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }

  @GetMapping("/{id}/result")
  @PreAuthorize("principal.name == {T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).FDPG_CLIENT_ID}")
  public QueryResult getQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }

  @GetMapping("/{id}/content")
  @PreAuthorize("hasRole({T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).KEYCLOAK_ALLOWED_ROLE}) "
      + "or principal.name == {T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).FDPG_CLIENT_ID}")
  public QueryResult getQueryContent(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }

  @PostMapping(path = "/template")
  @PreAuthorize("hasRole({T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).KEYCLOAK_ALLOWED_ROLE})")
  public ResponseEntity<Object> storeQueryTemplate(@Valid @RequestBody QueryTemplate queryTemplate,
      @Context HttpServletRequest httpServletRequest,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    Long queryId;
    try {
      queryId = queryHandlerService.storeQueryTemplate(queryTemplate, authorId);
    } catch (QueryTemplateException e) {
      log.error("Error while storing queryTemplate", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DataIntegrityViolationException e) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
        ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
        : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uriString = uriBuilder.replacePath("")
        .pathSegment("api", "v2", "query", "template", String.valueOf(queryId))
        .build()
        .toUriString();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.LOCATION, uriString);
    return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
  }

  @GetMapping(path = "/template/{queryId}")
  @PreAuthorize("hasRole({T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).KEYCLOAK_ALLOWED_ROLE})")
  public ResponseEntity<Object> getQueryTemplate(@PathVariable(value = "queryId") Long queryId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    try {
      var query = queryHandlerService.getQueryTemplate(queryId, authorId);
      QueryTemplate queryTemplate = queryHandlerService.convertTemplatePersistenceToApi(query);
      List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(queryTemplate);
      queryTemplate.setInvalidTerms(invalidTermCodes);
      return new ResponseEntity<>(queryTemplate, HttpStatus.OK);
    } catch (JsonProcessingException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (QueryTemplateException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping(path = "/template")
  @PreAuthorize("hasRole({T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).KEYCLOAK_ALLOWED_ROLE})")
  public ResponseEntity<Object> getQueryTemplates(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    var queries = queryHandlerService.getQueryTemplatesForAuthor(authorId);
    var ret = new ArrayList<QueryTemplate>();
    queries.forEach(q -> {
      try {
        QueryTemplate convertedQuery = queryHandlerService.convertTemplatePersistenceToApi(q);
        convertedQuery.setStructuredQuery(null);
        ret.add(convertedQuery);
      } catch (JsonProcessingException e) {
        log.error("Error converting query");
      }
    });
    return new ResponseEntity<>(ret, HttpStatus.OK);
  }

  @GetMapping(path = "/template/validate")
  @PreAuthorize("hasRole({T(de.numcodex.feasibility_gui_backend.query.v2.QueryHandlerRestController).KEYCLOAK_ALLOWED_ROLE})")
  public ResponseEntity<Object> validateTemplates(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, Principal principal) {

    String authorId = keycloakEnabled ? principal.getName()
        : getUserIdFromAuthorizationHeader(authorizationHeader);

    if (authorId == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    var queries = queryHandlerService.getQueryTemplatesForAuthor(authorId);
    var ret = new ArrayList<QueryTemplate>();
    queries.forEach(q -> {
      try {
        QueryTemplate convertedQuery = queryHandlerService.convertTemplatePersistenceToApi(q);
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
