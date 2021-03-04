package de.numcodex.feasibility_gui_backend.api;

import de.numcodex.feasibility_gui_backend.model.query.QueryDefinition;
import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.ResultLocation;
import de.numcodex.feasibility_gui_backend.service.QueryBuilderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 Rest Interface for the UI to send queries from the ui to the ui backend.
 */

@RequestMapping("api/v1/queryBuilder")
@RestController
public class QueryBuilderRestController {

  private final QueryBuilderService queryBuilderService;

  public QueryBuilderRestController(QueryBuilderService queryBuilderService) {
    this.queryBuilderService = queryBuilderService;
  }


  @PostMapping
  public ResultLocation runQuery(QueryDefinition query) {
    return queryBuilderService.runQuery(query);
  }

  @GetMapping(path = "{resultLocation}")
  public QueryResult getQueryResult(@PathVariable("resultLocation") ResultLocation resultLocation) {
    return queryBuilderService.getQueryResult(resultLocation);
  }

}
