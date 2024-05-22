package de.numcodex.feasibility_gui_backend.terminology.v3;

import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyItemDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyListItemDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologySearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v3/terminology/search")
@ConditionalOnExpression("${app.elastic.enabled}")
@CrossOrigin
public class TerminologyEsController {

  private ElasticsearchOperations operations;

  @Autowired
  public TerminologyEsController(ElasticsearchOperations operations) {
    this.operations = operations;
  }



  @GetMapping("")
  public OntologySearchResult searchOntologyItems(@RequestParam("searchterm") String keyword,
                                                            @RequestParam(value = "context", required = false) String context,
                                                            @RequestParam(value = "kdsModule", required = false) String kdsModule,
                                                            @RequestParam(value = "terminology", required = false) String terminology,
                                                            @RequestParam(value = "availability", required = false, defaultValue = "false") boolean availability,
                                                            @RequestParam(value = "limit", required = false, defaultValue = "0") int limit,
                                                            @RequestParam(value = "offset", required = false, defaultValue = "20") int offset) {
    var query = NativeQuery.builder()
        .withQuery(q -> q
            .queryString(qs -> qs
                .fields("name", "termcode")
                .query("*" + keyword + "*")
            )
        )
        .build();
    SearchHits<OntologyListItemDocument> searchHits = operations.search(query, OntologyListItemDocument.class);
    List<OntologyListItemDocument> ontologyItems = new ArrayList<>();
    searchHits.forEach(searchHit -> {
      ontologyItems.add(searchHit.getContent());
    });

    return OntologySearchResult.builder()
        .totalHits(searchHits.getTotalHits())
        .results(ontologyItems)
        .build();
  }

//  @GetMapping("/legacy")
//  public List<OntologyListItemDocument> searchOntologyItems2(@RequestParam("searchterm") String keyword) {
//    var query = NativeQuery.builder()
//        .withQuery(q -> q
//            .match(m -> m
//                .field("name")
//                .query(keyword)
//            )
//        )
//        .build();
//    SearchHits<OntologyListItemDocument> searchHits = operations.search(query, OntologyListItemDocument.class);
//    List<OntologyListItemDocument> uiProfiles = new ArrayList<>();
//    searchHits.forEach(searchHit -> {
//      uiProfiles.add(searchHit.getContent());
//    });
//    return uiProfiles;
//  }
//
//  @GetMapping("/wcsearch")
//  public List<OntologyListItemDocument> searchOntologyItemsWildcard(@RequestParam("searchterm") String keyword) {
//    var query = NativeQuery.builder()
//        .withQuery(q -> q
//            .wildcard(w -> w
//                .field("name")
//                .wildcard("*" + keyword + "*")
//                .caseInsensitive(true)
//            )
//        )
//        .build();
//    SearchHits<OntologyListItemDocument> searchHits = operations.search(query, OntologyListItemDocument.class);
//    List<OntologyListItemDocument> uiProfiles = new ArrayList<>();
//    searchHits.forEach(searchHit -> {
//      uiProfiles.add(searchHit.getContent());
//    });
//    return uiProfiles;
//  }

  // TODO
  @GetMapping("/filter")
  public String getAvailableFilters() {
    return "[{\"name\": \"Terminology\",\"values\":[\"ICD10\",\"SNOMED\",\"LOINC\"]}]";
  }

  @GetMapping("/{hash}")
  public OntologyItemDocument getOntologyItemByHash(@PathVariable("hash") String hash) {
    var ontologyItem = operations.get(hash, OntologyItemDocument.class);
    if (ontologyItem == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return ontologyItem;
  }
}
