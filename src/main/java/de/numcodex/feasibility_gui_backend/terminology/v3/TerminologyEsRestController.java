package de.numcodex.feasibility_gui_backend.terminology.v3;

import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("")
@ConditionalOnExpression("${app.elastic.enabled}")
@CrossOrigin
public class TerminologyEsRestController {

  private TerminologyEsService terminologyEsService;

  @Autowired
  public TerminologyEsRestController(TerminologyEsService terminologyEsService) {
    this.terminologyEsService = terminologyEsService;
  }

  @GetMapping("api/v3/terminology/search/filter")
  public List<TermFilter> getAvailableFilters() {
    return terminologyEsService.getAvailableFilters();
  }

  @GetMapping("api/v3/terminology/entry/search")
  public EsSearchResult searchOntologyItemsCriteriaQuery(@RequestParam("searchterm") String keyword,
                                                         @RequestParam(value = "contexts", required = false) List<String> contexts,
                                                         @RequestParam(value = "kds-modules", required = false) List<String> kdsModules,
                                                         @RequestParam(value = "terminologies", required = false) List<String> terminologies,
                                                         @RequestParam(value = "availability", required = false, defaultValue = "false") boolean availability,
                                                         @RequestParam(value = "page-size", required = false, defaultValue = "20") int pageSize,
                                                         @RequestParam(value = "page", required = false, defaultValue = "0") int page) {


    return terminologyEsService
        .performOntologySearchWithRepoAndPaging(keyword, contexts, kdsModules, terminologies, availability, pageSize, page);
  }

  @GetMapping("api/v3/terminology/entry/{hash}/relations")
  public OntologyItemRelationsDocument getOntologyItemRelationsByHash(@PathVariable("hash") String hash) {
    return terminologyEsService.getOntologyItemRelationsByHash(hash);
  }

  @GetMapping("api/v3/terminology/entry/{hash}")
  public EsSearchResultEntry getOntologyItemByHash(@PathVariable("hash") String hash) {
    return terminologyEsService.getSearchResultEntryByHash(hash);
  }
}
