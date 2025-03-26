package de.numcodex.feasibility_gui_backend.terminology.v5;

import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.CodeableConceptEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v5/codeable-concept")
@ConditionalOnExpression("${app.elastic.enabled}")
@CrossOrigin
public class CodeableConceptRestController {

  private final CodeableConceptService codeableConceptService;

  @Autowired
  public CodeableConceptRestController(CodeableConceptService codeableConceptService) {
    this.codeableConceptService = codeableConceptService;
  }

  @GetMapping(value = "/entry/search", produces = MediaType.APPLICATION_JSON_VALUE)
  public CcSearchResult searchOntologyItemsCriteriaQuery(@RequestParam("searchterm") String keyword,
                                                         @RequestParam(value = "value-sets", required = false) List<String> valueSets,
                                                         @RequestParam(value = "page-size", required = false, defaultValue = "20") int pageSize,
                                                         @RequestParam(value = "page", required = false, defaultValue = "0") int page) {

    return codeableConceptService
        .performCodeableConceptSearchWithRepoAndPaging(keyword, valueSets, pageSize, page);
  }

  @GetMapping(value = "/entry",  produces = MediaType.APPLICATION_JSON_VALUE)
  public List<CodeableConceptEntry> getCodeableConceptsByCode(@RequestParam List<String> ids) {
    return codeableConceptService.getSearchResultsEntryByIds(ids);
  }
}
