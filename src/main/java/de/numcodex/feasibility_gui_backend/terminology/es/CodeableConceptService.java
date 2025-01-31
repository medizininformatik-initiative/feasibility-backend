package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.numcodex.feasibility_gui_backend.terminology.api.CcSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.CodeableConceptEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.CodeableConceptDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
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
    List<CodeableConceptEntry> codeableConceptEntries = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> codeableConceptEntries.add(CodeableConceptEntry.of(hit.getContent())));
    return CcSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(codeableConceptEntries)
        .build();
  }

  public List<CodeableConceptEntry> getSearchResultsEntryByIds(List<String> ids) {
    var documents = repo.findAllById(ids);
    var codeableConceptEntries = new ArrayList<CodeableConceptEntry>();
    documents.forEach(d -> codeableConceptEntries.add(CodeableConceptEntry.of(d)));
    return codeableConceptEntries;
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

    BoolQuery outerBoolQuery;

    if (keyword.isEmpty()) {
      outerBoolQuery = new BoolQuery.Builder()
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();

    } else {
      // First the "upper" part of the query, when translations are present
      var mustMultiMatchQueryWithTranslations = new MultiMatchQuery.Builder()
          .query(keyword)
          .fields(List.of("display.de", "display.en", "termcode.code^2"))
          .build();

      var innerBoolQueryMatchTranslations = new BoolQuery.Builder()
          .must(List.of(mustMultiMatchQueryWithTranslations._toQuery()))
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();


      // The "lower" part that will only be considered when the translations are empty
      var mustMultiMatchQueryWithOriginal = new MultiMatchQuery.Builder()
          .query(keyword)
          .fields(List.of("display.original", "termcode.code^2"))
          .build();

      var innerBoolQueryMatchOriginal = new BoolQuery.Builder()
          .must(List.of(mustMultiMatchQueryWithOriginal._toQuery()))
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();

      // Combine both parts in the top level bool query
      outerBoolQuery = new BoolQuery.Builder()
          .should(List.of(innerBoolQueryMatchTranslations._toQuery(), innerBoolQueryMatchOriginal._toQuery()))
          .minimumShouldMatch("1")
          .build();
    }

    var query = new NativeQueryBuilder()
        .withQuery(outerBoolQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    log.info(query.getQuery().toString());

    return operations.search(query, CodeableConceptDocument.class);
  }
}
