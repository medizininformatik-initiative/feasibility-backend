package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyListItemEsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class TerminologyEsService {
  private ElasticsearchOperations operations;

  @Value("${app.elastic.filter}")
  private String[] filterFields;

  private OntologyItemEsRepository ontologyItemEsRepository;

  private OntologyListItemEsRepository ontologyListItemEsRepository;

  @Autowired
  public TerminologyEsService(ElasticsearchOperations operations, OntologyItemEsRepository ontologyItemEsRepository, OntologyListItemEsRepository ontologyListItemEsRepository) {
    this.operations = operations;
    this.ontologyItemEsRepository = ontologyItemEsRepository;
    this.ontologyListItemEsRepository = ontologyListItemEsRepository;
  }

  public EsSearchResultEntry getSearchResultEntryByHash(String hash) {
    var ontologyItem = ontologyListItemEsRepository.findById(hash).orElseThrow(OntologyItemNotFoundException::new);
    return EsSearchResultEntry.of(ontologyItem);
  }

  public List<TermFilter> getAvailableFilters() {
    var filterTerms = List.of(filterFields);
    var list = new ArrayList<TermFilter>();

    for (var term : filterTerms) {
      list.add(getFilter(term));
    }

    // Hardcode availability filter. It is not yet decided if and how this will be available anyways. TODO!
    list.add(TermFilter.builder()
        .type("boolean")
        .name("availability")
        .values(List.of())
        .build());

    return list;
  }

  public OntologySearchResult performOntologySearchWithRepo(String keyword,
                                                    @Nullable String context,
                                                    @Nullable String kdsModule,
                                                    @Nullable String terminology,
                                                    @Nullable boolean availability,
                                                    @Nullable int limit,
                                                    @Nullable int offset) {

    var searchHits = ontologyListItemEsRepository.findByNameContainingIgnoreCaseOrTermcodeContainingIgnoreCase(keyword, keyword);
    List<OntologyListItemDocument> ontologyItems = new ArrayList<>();
    searchHits.forEach(searchHit -> {
      ontologyItems.add(searchHit.getContent());
    });
    return OntologySearchResult.builder()
        .totalHits(searchHits.getTotalHits())
        .results(ontologyItems)
        .build();
  }

  /*
  I know this is kinda stupid, but it works for now (availability has to be included though).
   */
  public EsSearchResult performOntologySearchWithRepoAndPaging(String keyword,
                                                               @Nullable List<String> context,
                                                               @Nullable List<String> kdsModule,
                                                               @Nullable List<String> terminology,
                                                               @Nullable boolean availability,
                                                               @Nullable int pageSize,
                                                               @Nullable int page) {


    List<Pair<String, List<String>>> filterList = new ArrayList<>();
    if (context != null) {
      filterList.add(Pair.of("context.code", context));
    }
    if (kdsModule != null) {
      filterList.add(Pair.of("kds_module", kdsModule));
    }
    if (terminology != null) {
      filterList.add(Pair.of("terminology", terminology));
    }

    Page<OntologyListItemDocument> searchHitPage;

    switch (filterList.size()) {
      case 0 -> {
        searchHitPage = ontologyListItemEsRepository
            .findByNameOrTermcodeMultiMatch0Filters(keyword,
                PageRequest.of(page, pageSize));
      }
      case 1 -> {searchHitPage = ontologyListItemEsRepository
          .findByNameOrTermcodeMultiMatch1Filter(keyword,
              filterList.get(0).getFirst(),
              filterList.get(0).getSecond(),
              PageRequest.of(page, pageSize));
      }
      case 2 -> {searchHitPage = ontologyListItemEsRepository
          .findByNameOrTermcodeMultiMatch2Filters(keyword,
              filterList.get(0).getFirst(),
              filterList.get(0).getSecond(),
              filterList.get(1).getFirst(),
              filterList.get(1).getSecond(),
              PageRequest.of(page, pageSize));
      }
      case 3 -> {searchHitPage = ontologyListItemEsRepository
          .findByNameOrTermcodeMultiMatch3Filters(keyword,
              filterList.get(0).getFirst(),
              filterList.get(0).getSecond(),
              filterList.get(1).getFirst(),
              filterList.get(1).getSecond(),
              filterList.get(2).getFirst(),
              filterList.get(2).getSecond(),
              PageRequest.of(page, pageSize));
      }
      default -> {searchHitPage = ontologyListItemEsRepository
          .findByNameOrTermcodeMultiMatch3Filters(keyword,
              filterList.get(0).getFirst(),
              filterList.get(0).getSecond(),
              filterList.get(1).getFirst(),
              filterList.get(1).getSecond(),
              filterList.get(2).getFirst(),
              filterList.get(2).getSecond(),
              PageRequest.of(page, pageSize));
      }
    }
    List<EsSearchResultEntry> ontologyItems = new ArrayList<>();

    searchHitPage.getContent().forEach(hit -> ontologyItems.add(EsSearchResultEntry.of(hit)));
    return EsSearchResult.builder()
        .totalHits(searchHitPage.getTotalElements())
        .results(ontologyItems)
        .build();
  }

  public OntologySearchResult performOntologySearch(String keyword,
                                                    @Nullable String context,
                                                    @Nullable String kdsModule,
                                                    @Nullable String terminology,
                                                    @Nullable boolean availability,
                                                    @Nullable int pageSize,
                                                    @Nullable int page) {

    //    var query = buildCriteriaQuery(keyword, context, kdsModule, terminology, availability, pageSize, page);
    var query = buildNativeQuery(keyword, context, kdsModule, terminology, availability, pageSize, page);
    var searchHits = operations.search(query, OntologyListItemDocument.class);
    List<OntologyListItemDocument> ontologyItems = new ArrayList<>();
    searchHits.forEach(searchHit -> {
      ontologyItems.add(searchHit.getContent());
    });

//    ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
//    List<StringTermsBucket> buckets = aggregations.aggregationsAsMap().get("context").aggregation().getAggregate().sterms().buckets().array();
//
//    buckets.forEach(b -> {
//      System.out.println(b.key()._toJsonString() + "(" + b.docCount() + ")");
//    });

    return OntologySearchResult.builder()
        .totalHits(searchHits.getTotalHits())
        .results(ontologyItems)
        .build();
  }

  private TermFilter getFilter(String term) {
    var aggQuery = NativeQuery.builder()
        .withAggregation(term, Aggregation.of(a -> a
            .terms(ta -> ta.field(term))))
        .build();

    SearchHits<OntologyListItemDocument> searchHits = operations.search(aggQuery, OntologyListItemDocument.class);
    ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
    assert aggregations != null;
    List<StringTermsBucket> buckets = aggregations.aggregationsAsMap().get(term).aggregation().getAggregate().sterms().buckets().array();
    List<TermFilterValue> termFilterValues = new ArrayList<>();

    buckets.forEach(b -> {
      if (!b.key().stringValue().isEmpty()) {
        termFilterValues.add(TermFilterValue.builder().label(b.key().stringValue()).count(b.docCount()).build());
      }
    });

    return TermFilter.builder()
        .name(term)
        .type("selectable-concept")
        .values(termFilterValues)
        .build();
  }

  private CriteriaQuery buildCriteriaQuery(String keyword,
                                          String context,
                                          String kdsModule,
                                          String terminology,
                                          boolean availability) {
    var criteria = new Criteria("name")
        .expression(keyword)
        .or("termcode")
        .expression(keyword);

    if (context != null && !context.isEmpty()) {
      criteria = criteria.and("context").is(context);
    }
    if (kdsModule != null && !kdsModule.isEmpty()) {
      criteria = criteria.and("kdsModule").is(kdsModule);
    }
    if (terminology != null && !terminology.isEmpty()) {
      criteria = criteria.and("terminology").is(terminology);
    }
    if (availability) {
      // TODO maybe add a configurable threshold here?
      criteria = criteria.and("availability").greaterThan(0);
    }

    return new CriteriaQuery(criteria);
  }

  private NativeQuery buildNativeQuery(String keyword,
                                      String context,
                                      String kdsModule,
                                      String terminology,
                                      boolean availability,
                                      int pageSize,
                                      int page) {

    var criteriaQuery = buildCriteriaQuery(keyword, context, kdsModule, terminology, availability);



//    AggregationBuilders
//        .terms()
//        .field("context").name("context")
//        .field("kdsModule").name("kdsModule")
//        .field("terminology").name("terminology")
//        .build();

    Pageable pageable = Pageable.ofSize(pageSize);
    criteriaQuery.setPageable(pageable);

    var query = NativeQuery.builder()
        .withQuery(criteriaQuery)
        .withPageable(page > 0 ? pageable.withPage(page) : pageable.first())
        .withAggregation("context", Aggregation.of(a -> a
            .terms(ta -> ta.field("context"))))
        .withAggregation("kdsModule", Aggregation.of(a -> a
            .terms(ta -> ta.field("kdsModule"))))
        .withAggregation("terminology", Aggregation.of(a -> a
            .terms(ta -> ta.field("terminology"))))
        .build();
    return query;
  }

  public OntologyItemRelationsDocument getOntologyItemRelationsByHash(String hash) {
    var ontologyItem = ontologyItemEsRepository.findById(hash).orElseThrow(OntologyItemNotFoundException::new);
    return OntologyItemRelationsDocument.builder()
        .translations(ontologyItem.getTranslations())
        .parents(ontologyItem.getParents())
        .children(ontologyItem.getChildren())
        .relatedTerms(ontologyItem.getRelatedTerms())
        .build();
  }
}
