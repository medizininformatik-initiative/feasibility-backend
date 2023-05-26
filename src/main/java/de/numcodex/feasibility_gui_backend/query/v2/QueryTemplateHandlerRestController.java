package de.numcodex.feasibility_gui_backend.query.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.query.QueryHandlerService;
import de.numcodex.feasibility_gui_backend.query.api.QueryTemplate;
import de.numcodex.feasibility_gui_backend.query.templates.QueryTemplateException;
import de.numcodex.feasibility_gui_backend.terminology.validation.TermCodeValidation;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/*
Rest Interface for the UI to send and receive query templates from the backend.
*/
@RequestMapping("api/v2/query/template")
@RestController("QueryTemplateHandlerRestController-v2")
@Slf4j
@CrossOrigin(origins = "${cors.allowedOrigins}", exposedHeaders = "Location")
public class QueryTemplateHandlerRestController {

  private final QueryHandlerService queryHandlerService;
  private final TermCodeValidation termCodeValidation;
  private final String apiBaseUrl;

  public QueryTemplateHandlerRestController(QueryHandlerService queryHandlerService,
      TermCodeValidation termCodeValidation, @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.termCodeValidation = termCodeValidation;
    this.apiBaseUrl = apiBaseUrl;
  }

  @PostMapping(path = "")
  public ResponseEntity<Object> storeQueryTemplate(@Valid @RequestBody QueryTemplate queryTemplate,
      @Context HttpServletRequest httpServletRequest, Principal principal) {

    Long queryId;
    try {
      queryId = queryHandlerService.storeQueryTemplate(queryTemplate, principal.getName());
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

  @GetMapping(path = "/{queryId}")
  public ResponseEntity<Object> getQueryTemplate(@PathVariable(value = "queryId") Long queryId,
      Principal principal) {

    try {
      var query = queryHandlerService.getQueryTemplate(queryId, principal.getName());
      var queryTemplate = queryHandlerService.convertTemplatePersistenceToApi(query);
      List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(
          queryTemplate.content());
      var queryTemplateWithInvalidTermCodes = new QueryTemplate(
          queryTemplate.id(),
          queryTemplate.content(),
          queryTemplate.label(),
          queryTemplate.comment(),
          queryTemplate.lastModified(),
          queryTemplate.createdBy(),
          invalidTermCodes,
          queryTemplate.isValid()
      );
      return new ResponseEntity<>(queryTemplateWithInvalidTermCodes, HttpStatus.OK);
    } catch (JsonProcessingException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (QueryTemplateException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping(path = "")
  public ResponseEntity<Object> getQueryTemplates(Principal principal) {

    var queries = queryHandlerService.getQueryTemplatesForAuthor(principal.getName());
    var ret = new ArrayList<QueryTemplate>();
    queries.forEach(q -> {
      try {
        QueryTemplate convertedQuery = queryHandlerService.convertTemplatePersistenceToApi(q);
        QueryTemplate convertedQueryWithoutContent = new QueryTemplate(
            convertedQuery.id(),
            null,
            convertedQuery.label(),
            convertedQuery.comment(),
            convertedQuery.lastModified(),
            convertedQuery.createdBy(),
            convertedQuery.invalidTerms(),
            convertedQuery.isValid()
        );
        ret.add(convertedQueryWithoutContent);
      } catch (JsonProcessingException e) {
        log.error("Error converting query");
      }
    });
    return new ResponseEntity<>(ret, HttpStatus.OK);
  }

  @GetMapping(path = "/validate")
  public ResponseEntity<Object> validateTemplates(Principal principal) {

    var queries = queryHandlerService.getQueryTemplatesForAuthor(principal.getName());
    var ret = new ArrayList<QueryTemplate>();
    queries.forEach(q -> {
      try {
        QueryTemplate convertedQuery = queryHandlerService.convertTemplatePersistenceToApi(q);
        List<TermCode> invalidTermCodes = termCodeValidation.getInvalidTermCodes(
            convertedQuery.content());
        QueryTemplate convertedQueryWithoutContent = new QueryTemplate(
            convertedQuery.id(),
            null,
            convertedQuery.label(),
            convertedQuery.comment(),
            convertedQuery.lastModified(),
            convertedQuery.createdBy(),
            convertedQuery.invalidTerms(),
            invalidTermCodes.isEmpty()
        );
        ret.add(convertedQueryWithoutContent);
      } catch (JsonProcessingException e) {
        log.error("Error converting query");
      }
    });
    return new ResponseEntity<>(ret, HttpStatus.OK);
  }

}
