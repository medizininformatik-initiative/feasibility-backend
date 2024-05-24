package de.numcodex.feasibility_gui_backend.terminology.v3;

import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v3/terminology/search")
@ConditionalOnExpression("${app.elastic.enabled}")
@CrossOrigin
public class TerminologyEsController {

  private TerminologyEsService terminologyEsService;

  @Autowired
  public TerminologyEsController(TerminologyEsService terminologyEsService) {
    this.terminologyEsService = terminologyEsService;
  }

  @GetMapping("")
  public OntologySearchResult searchOntologyItemsCriteriaQuery(@RequestParam("searchterm") String keyword,
                                                  @RequestParam(value = "context", required = false) String context,
                                                  @RequestParam(value = "kdsModule", required = false) String kdsModule,
                                                  @RequestParam(value = "terminology", required = false) String terminology,
                                                  @RequestParam(value = "availability", required = false, defaultValue = "false") boolean availability,
                                                  @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
                                                  @RequestParam(value = "page", required = false, defaultValue = "0") int page) {

//    return terminologyEsService
//        .performOntologySearch(keyword, context, kdsModule, terminology, availability, pageSize, page);
    return terminologyEsService
        .performOntologySearchWithRepoAndPaging(keyword, context, kdsModule, terminology, availability, pageSize, page);
  }

  @GetMapping("/filter")
  public List<TermFilter> getAvailableFilters() {
    return terminologyEsService.getAvailableFilters();
  }

  @GetMapping("/{hash}")
  public OntologyItemDocument getOntologyItemByHash(@PathVariable("hash") String hash) {
    return terminologyEsService.getOntologyItemByHash(hash);
  }
}
