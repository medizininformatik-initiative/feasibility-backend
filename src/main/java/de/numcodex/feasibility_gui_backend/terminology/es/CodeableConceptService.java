package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class CodeableConceptService {
  private ElasticsearchOperations operations;

  private CodeableConceptEsRepository repo;

  @Autowired
  public CodeableConceptService(ElasticsearchOperations operations, CodeableConceptEsRepository repo) {
    this.operations = operations;
    this.repo = repo;
  }

  public CcSearchResult performCodeableConceptSearchWithRepoAndPaging(String keyword,
                                                               @Nullable List<String> valueSets,
                                                               @Nullable int pageSize,
                                                               @Nullable int page) {

    List<Pair<String, List<String>>> filterList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(valueSets)) {
      filterList.add(Pair.of("value_sets", valueSets));
    }

    var searchHitPage = findByCodeOrDisplay(keyword, filterList, PageRequest.of(page, pageSize));
    List<TermCode> codeableConceptEntries = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> codeableConceptEntries.add(hit.getContent().termCode()));
    return CcSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(codeableConceptEntries)
        .build();
  }

  public TermCode getSearchResultEntryByCode(String code) {
    return repo.findById(code).orElseThrow(OntologyItemNotFoundException::new).termCode();
  }

  private SearchHits<CodeableConceptDocument> findByCodeOrDisplay(String keyword,
                                                                  List<Pair<String,List<String>>> filterList,
                                                                  PageRequest pageRequest) {
    List<Query> filterTerms = new ArrayList<>();

    if (!filterList.isEmpty()) {
      var fieldValues = new ArrayList<FieldValue>();
      filterList.forEach(f -> {
        f.getSecond().forEach(s -> {
          fieldValues.add(new FieldValue.Builder().stringValue(s).build());
        });
        filterTerms.add(new TermsQuery.Builder()
            .field(f.getFirst())
            .terms(new TermsQueryField.Builder().value(fieldValues).build())
            .build()._toQuery());
      });
    }

    var mmQuery = new MultiMatchQuery.Builder()
        .query(keyword)
        .fields(List.of("termcode.display", "termcode.code^2"))
        .build();

    var boolQuery = new BoolQuery.Builder()
        .must(List.of(mmQuery._toQuery()))
        .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
        .build();

    var query = new NativeQueryBuilder()
        .withQuery(boolQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    return operations.search(query, CodeableConceptDocument.class);
  }
}
