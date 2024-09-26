package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@Slf4j
@ConditionalOnExpression("${app.elastic.enabled}")
public class TerminologyEsService {
  private ElasticsearchOperations operations;

  private String[] filterFields;

  private OntologyItemEsRepository ontologyItemEsRepository;

  private OntologyListItemEsRepository ontologyListItemEsRepository;

  @Autowired
  public TerminologyEsService(@Value("${app.elastic.filter}") String[] filterFields, ElasticsearchOperations operations, OntologyItemEsRepository ontologyItemEsRepository, OntologyListItemEsRepository ontologyListItemEsRepository) {
    this.filterFields = filterFields;
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

  public EsSearchResult performOntologySearchWithPaging(String keyword,
                                                        @Nullable List<String> criteriaSets,
                                                        @Nullable List<String> context,
                                                        @Nullable List<String> kdsModule,
                                                        @Nullable List<String> terminology,
                                                        @Nullable boolean availability,
                                                        @Nullable int pageSize,
                                                        @Nullable int page) {


    List<Pair<String, List<String>>> filterList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(criteriaSets)) {
      filterList.add(Pair.of("criteria_sets", criteriaSets));
    }
    if (!CollectionUtils.isEmpty(context)) {
      filterList.add(Pair.of("context.code", context));
    }
    if (!CollectionUtils.isEmpty(kdsModule)) {
      filterList.add(Pair.of("kds_module", kdsModule));
    }
    if (!CollectionUtils.isEmpty(terminology)) {
      filterList.add(Pair.of("terminology", terminology));
    }

    SearchHits<OntologyListItemDocument> searchHitPage = findByNameOrTermcode(
        keyword,
        filterList,
        availability,
        PageRequest.of(page, pageSize)
    );

    List<EsSearchResultEntry> ontologyItems = new ArrayList<>();

    searchHitPage.getSearchHits().forEach(hit -> ontologyItems.add(EsSearchResultEntry.of(hit.getContent())));
    return EsSearchResult.builder()
        .totalHits(searchHitPage.getTotalHits())
        .results(ontologyItems)
        .build();
  }

  private SearchHits<OntologyListItemDocument> findByNameOrTermcode(String keyword,
                                                                    List<Pair<String,List<String>>> filterList,
                                                                    boolean availability,
                                                                    PageRequest pageRequest) {

    List<Query> filterTerms = new ArrayList<>();

    if (availability) {
      var availabilityFilter = new RangeQuery.Builder()
          .field("availability")
          .gt(JsonData.of("0"))
          .build();
      filterTerms.add(availabilityFilter._toQuery());
    }

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

    BoolQuery boolQuery;

    if (keyword.isEmpty()) {
      boolQuery = new BoolQuery.Builder()
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();
    } else {
      var mmQuery = new MultiMatchQuery.Builder()
          .query(keyword)
          .fields(List.of("name", "termcode^2"))
          .build();

      boolQuery = new BoolQuery.Builder()
          .must(List.of(mmQuery._toQuery()))
          .filter(filterTerms.isEmpty() ? List.of() : filterTerms)
          .build();
    }

    var innerQuery = new NativeQueryBuilder()
        .withQuery(boolQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    var inlineScript = new InlineScript.Builder()
        .source("doc['availability'].value == 0 ? _score : _score + 100")
        .build();

    var availabilityScoreScript = new Script.Builder()
        .inline(inlineScript)
        .build();

    var function = FunctionScoreBuilders.scriptScore()
        .script(availabilityScoreScript)
        .build();

    var functionList = List.of(function._toFunctionScore());

    var functionScoreQuery = new FunctionScoreQuery.Builder()
        .query(innerQuery.getQuery())
        .functions(functionList)
        .boostMode(FunctionBoostMode.Replace)
        .build();

    var finalQuery = new NativeQueryBuilder()
        .withQuery(functionScoreQuery._toQuery())
        .withPageable(pageRequest)
        .build();

    return operations.search(finalQuery, OntologyListItemDocument.class);

  }

  public OntologyItemRelationsDocument getOntologyItemRelationsByHash(String hash) {
    var ontologyItem = ontologyItemEsRepository.findById(hash).orElseThrow(OntologyItemNotFoundException::new);
    return OntologyItemRelationsDocument.builder()
        .translations(ontologyItem.translations())
        .parents(ontologyItem.parents())
        .children(ontologyItem.children())
        .relatedTerms(ontologyItem.relatedTerms())
        .build();
  }


  private TermFilter getFilter(String termApi) {
    final var termElastic = termApi.equalsIgnoreCase("context") ? "context.code" : termApi;
    var aggQuery = NativeQuery.builder()
        .withAggregation(termElastic, Aggregation.of(a -> a
            .terms(ta -> ta.field(termElastic))))
        .build();

    SearchHits<OntologyListItemDocument> searchHits = operations.search(aggQuery, OntologyListItemDocument.class);
    ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
    assert aggregations != null;
    List<StringTermsBucket> buckets = aggregations.aggregationsAsMap().get(termElastic).aggregation().getAggregate().sterms().buckets().array();
    List<TermFilterValue> termFilterValues = new ArrayList<>();

    buckets.forEach(b -> {
      if (!b.key().stringValue().isEmpty()) {
        termFilterValues.add(TermFilterValue.builder().label(b.key().stringValue()).count(b.docCount()).build());
      }
    });

    return TermFilter.builder()
        .name(termApi)
        .type("selectable-concept")
        .values(termFilterValues)
        .build();
  }
}
