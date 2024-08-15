package de.numcodex.feasibility_gui_backend.terminology.es;

import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class CodeableConceptEsService {
  private ElasticsearchOperations operations;

  private CodeableConceptEsRepository repo;

  @Autowired
  public CodeableConceptEsService(ElasticsearchOperations operations, CodeableConceptEsRepository repo) {
    this.operations = operations;
    this.repo = repo;
  }

  public CcSearchResult performCodeableConceptSearchWithRepoAndPaging(String keyword,
                                                               @Nullable List<String> valueSets,
                                                               @Nullable int pageSize,
                                                               @Nullable int page) {
    Page<CodeableConceptDocument> searchHitPage;

    if (valueSets != null && !valueSets.isEmpty()) {
      searchHitPage = repo
          .findByNameOrTermcodeMultiMatch1Filter(keyword,
              "valuesets",
              valueSets,
              PageRequest.of(page, pageSize));
    } else {
      searchHitPage = repo
          .findByNameOrTermcodeMultiMatch0Filters(keyword,
              PageRequest.of(page, pageSize));
    }
    List<CcSearchResultEntry> codeableConceptEntries = new ArrayList<>();

    searchHitPage.getContent().forEach(hit -> codeableConceptEntries.add(CcSearchResultEntry.of(hit)));
    return CcSearchResult.builder()
        .totalHits(searchHitPage.getTotalElements())
        .results(codeableConceptEntries)
        .build();
  }

  public CcSearchResultEntry getSearchResultEntryByCode(String code) {
    var ccItem = repo.findById(code).orElseThrow(OntologyItemNotFoundException::new);
    return CcSearchResultEntry.of(ccItem);
  }
}
