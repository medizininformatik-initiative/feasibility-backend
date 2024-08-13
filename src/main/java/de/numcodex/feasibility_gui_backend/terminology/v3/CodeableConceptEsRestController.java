package de.numcodex.feasibility_gui_backend.terminology.v3;

import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.CodeableConceptEsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v3/codeable_concept/entry")
@ConditionalOnExpression("${app.elastic.enabled}")
@CrossOrigin
public class CodeableConceptEsRestController {

  private CodeableConceptEsService codeableConceptEsService;

  @Autowired
  public CodeableConceptEsRestController(CodeableConceptEsService codeableConceptEsService) {
    this.codeableConceptEsService = codeableConceptEsService;
  }

  @GetMapping("/search")
  public CcSearchResult searchOntologyItemsCriteriaQuery(@RequestParam("searchterm") String keyword,
                                                         @RequestParam(value = "valueSets", required = false) List<String> valueSets,
                                                         @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
                                                         @RequestParam(value = "page", required = false, defaultValue = "0") int page) {

    return codeableConceptEsService
        .performCodeableConceptSearchWithRepoAndPaging(keyword, valueSets, pageSize, page);
  }

  @GetMapping("/{code}")
  public CcSearchResultEntry getCodeableConceptByCode(@PathVariable("code") String code) {
    return codeableConceptEsService.getSearchResultEntryByCode(code);
  }
}
