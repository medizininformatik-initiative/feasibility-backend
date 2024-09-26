package de.numcodex.feasibility_gui_backend.terminology.es;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.*;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyListItemEsRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Tag("terminology")
@Tag("elasticsearch")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TerminologyEsServiceTest {

  private String[] filterFields = new String[] {"foo", "bar", "baz"};
  @Mock
  private ElasticsearchOperations operations;
  @Mock
  private OntologyItemEsRepository ontologyItemEsRepository;
  @Mock
  private OntologyListItemEsRepository ontologyListItemEsRepository;
  @Mock
  private SearchHits<OntologyListItemDocument> searchHits;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ElasticsearchAggregations elasticsearchAggregations;
  @Mock
  private List<StringTermsBucket> stringTermsBuckets;

  @InjectMocks
  private TerminologyEsService terminologyEsService;

  private TerminologyEsService createTerminologyEsService() {
    return new TerminologyEsService(filterFields, operations, ontologyItemEsRepository, ontologyListItemEsRepository);
  }

  @BeforeEach
  void setUp() {
    Mockito.reset(operations, ontologyItemEsRepository, ontologyListItemEsRepository);
    terminologyEsService = createTerminologyEsService();
  }

  @Test
  void testGetSearchResultEntryByHash() {
    String id = UUID.randomUUID().toString();
    OntologyListItemDocument dummyOntologyListItem = createDummyOntologyListItem(id);
    doReturn(Optional.of(dummyOntologyListItem)).when(ontologyListItemEsRepository).findById(any(String.class));

    var searchResultEntry = assertDoesNotThrow(() -> terminologyEsService.getSearchResultEntryByHash(id));

    assertThat(searchResultEntry).isNotNull();
    assertThat(searchResultEntry.id()).isEqualTo(id);
    assertThat(searchResultEntry.terminology()).isEqualTo(dummyOntologyListItem.terminology());
    assertThat(searchResultEntry.name()).isEqualTo(dummyOntologyListItem.name());
    assertThat(searchResultEntry.kdsModule()).isEqualTo(dummyOntologyListItem.kdsModule());
    assertThat(searchResultEntry.availability()).isEqualTo(dummyOntologyListItem.availability());
    assertThat(searchResultEntry.context()).isEqualTo(dummyOntologyListItem.context().code());
    assertThat(searchResultEntry.termcode()).isEqualTo(dummyOntologyListItem.termcode());
  }

  @Test
  void testGetSearchResultEntryByHash_throwsOnNotFound() {
    doReturn(Optional.empty()).when(ontologyListItemEsRepository).findById(any(String.class));

    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getSearchResultEntryByHash("id"));
  }

  @Test
  void testGetAvailableFilters() {
    var expectedTermFiltersList = createTermFilterList(filterFields);

    doReturn(searchHits).when(operations).search(any(NativeQuery.class), any());
    doReturn(elasticsearchAggregations).when(searchHits).getAggregations();
    // This fails when written as doReturn()...when(), but works in this order...so...
    when(elasticsearchAggregations.aggregationsAsMap().get(any(String.class)).aggregation().getAggregate().sterms()
        .buckets().array()).thenReturn(List.of(createStringTermsBucket()));

    var filters = terminologyEsService.getAvailableFilters();

    assertThat(filters.size()).isEqualTo(expectedTermFiltersList.size());
    assertThat(filters).containsAll(expectedTermFiltersList);
  }

  private static Stream<Arguments> generateArgumentsForTestPerformOntologySearchWithPaging() {
    var booleanList = List.of(true, false);
    var list = new ArrayList<Arguments>();

    for (boolean availability : booleanList) {
      list.add(Arguments.of(null, null, null, null, availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), null, null, availability, 20, 0));
      list.add(Arguments.of(null, null, List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(null, null, null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(null, null, List.of("bar"), List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(null, List.of("foo"), List.of("bar"), List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, null, null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), null, null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), List.of("bar"), null, availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), null, List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), null, List.of("bar"), List.of("baz"), availability, 20, 0));
      list.add(Arguments.of(List.of("foobar"), List.of("foo"), List.of("bar"), List.of("baz"), availability, 20, 0));
    }

    return list.stream();
  }

  @ParameterizedTest
  @MethodSource("generateArgumentsForTestPerformOntologySearchWithPaging")
  void testPerformOntologySearchWithPaging(List<String> criteriaSets, List<String> context, List<String> kdsModule, List<String> terminology, Boolean availability, Integer pageSize, Integer page) {
    int totalHits = new Random().nextInt(100);
    SearchHits<OntologyListItemDocument> dummySearchHitsPage = createDummySearchHitsPage(totalHits);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var searchResult = assertDoesNotThrow(
        () -> terminologyEsService.performOntologySearchWithPaging(
            "foobar", criteriaSets, context, kdsModule, terminology, availability, pageSize, page)
    );

    assertThat(searchResult.totalHits()).isEqualTo(totalHits);
    assertThat(searchResult.results().size()).isEqualTo(dummySearchHitsPage.getTotalHits());
    assertThat(searchResult.results()).containsExactlyInAnyOrderElementsOf(dummySearchHitsPage.getSearchHits().stream().map(sh -> EsSearchResultEntry.of(sh.getContent())).toList());
  }

  @ParameterizedTest
  @MethodSource("generateArgumentsForTestPerformOntologySearchWithPaging")
  void testPerformOntologySearchWithPagingEmptyKeyword(List<String> criteriaSets, List<String> context, List<String> kdsModule, List<String> terminology, Boolean availability, Integer pageSize, Integer page) {
    int totalHits = new Random().nextInt(100);
    SearchHits<OntologyListItemDocument> dummySearchHitsPage = createDummySearchHitsPage(totalHits);
    doReturn(dummySearchHitsPage).when(operations).search(any(NativeQuery.class), any(Class.class));

    var searchResult = assertDoesNotThrow(
        () -> terminologyEsService.performOntologySearchWithPaging(
            "", criteriaSets, context, kdsModule, terminology, availability, pageSize, page)
    );

    assertThat(searchResult.totalHits()).isEqualTo(totalHits);
    assertThat(searchResult.results().size()).isEqualTo(dummySearchHitsPage.getTotalHits());
    assertThat(searchResult.results()).containsExactlyInAnyOrderElementsOf(dummySearchHitsPage.getSearchHits().stream().map(sh -> EsSearchResultEntry.of(sh.getContent())).toList());
  }

  @Test
  void testPerformOntologySearchWithRepoAndPaging_throwsOnInvalidPageSize() {
    assertThrows(IllegalArgumentException.class, () -> terminologyEsService.performOntologySearchWithPaging(
            "foobar", null, null, null, null, false, 0, 0)
    );
  }

  @Test
  void testGetOntologyItemRelationsByHash_succeeds() {
    String id = UUID.randomUUID().toString();
    OntologyItemDocument dummyOntologyItem = createDummyOntologyItem(id);
    doReturn(Optional.of(dummyOntologyItem)).when(ontologyItemEsRepository).findById(any(String.class));

    var ontologyItemRelationsDocument = assertDoesNotThrow(() -> terminologyEsService.getOntologyItemRelationsByHash(id));
    assertThat(ontologyItemRelationsDocument).isNotNull();
    assertThat(ontologyItemRelationsDocument.relatedTerms()).isEqualTo(dummyOntologyItem.relatedTerms());
    assertThat(ontologyItemRelationsDocument.children()).isEqualTo(dummyOntologyItem.children());
    assertThat(ontologyItemRelationsDocument.parents()).isEqualTo(dummyOntologyItem.parents());
    assertThat(ontologyItemRelationsDocument.translations()).isEqualTo(dummyOntologyItem.translations());
  }

  @Test
  void testGetOntologyItemRelationsByHash_throwsOnNotFound() {
    doReturn(Optional.empty()).when(ontologyItemEsRepository).findById(any(String.class));

    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getOntologyItemRelationsByHash("id"));
  }

  private OntologyListItemDocument createDummyOntologyListItem(String id) {
    TermCode termCode = createDummyTermCode();

    return OntologyListItemDocument.builder()
        .id(id)
        .name("Some Name")
        .availability(1)
        .context(termCode)
        .terminology("Some Terminology")
        .termcode("Some Termcode")
        .kdsModule("Some KDS Module")
        .build();
  }

  private OntologyItemDocument createDummyOntologyItem(String id) {
    TermCode termCode = createDummyTermCode();
    Collection<Translation> translations = List.of(createDummyTranslation(), createDummyTranslation(), createDummyTranslation());
    Collection<Relative> parents = List.of(createDummyRelative(), createDummyRelative());
    Collection<Relative> children = List.of(createDummyRelative(), createDummyRelative(), createDummyRelative(), createDummyRelative());
    Collection<Relative> relatedTerms = List.of(createDummyRelative(), createDummyRelative());

    return OntologyItemDocument.builder()
        .id(id)
        .name("Some Name")
        .availability(1)
        .context(termCode)
        .terminology("Some Terminology")
        .termcode("Some Termcode")
        .kdsModule("Some KDS Module")
        .translations(translations)
        .parents(parents)
        .children(children)
        .relatedTerms(relatedTerms)
        .build();
  }

  private Translation createDummyTranslation() {
    return Translation.builder()
        .lang("de")
        .value("Lorem Ipsum")
        .build();
  }

  private Relative createDummyRelative() {
    return Relative.builder()
        .contextualizedTermcodeHash(UUID.randomUUID().toString())
        .name("some-random-name")
        .build();
  }

  private TermCode createDummyTermCode() {
    return TermCode.builder()
        .code("abc-123")
        .version("2024")
        .system("some-system")
        .display("this is a dummy termcode")
        .build();
  }

  private StringTermsBucket createStringTermsBucket() {
    StringTermsBucket.Builder builder = new StringTermsBucket.Builder();
    for (String filter : filterFields) {
      builder.key(filter);
    }
    builder.docCount(filterFields.length);
    return builder.build();
  }

  private List<TermFilter> createTermFilterList(String[] values) {
    var termFilters = new ArrayList<TermFilter>();
    for (String term : values) {
      termFilters.add(
          TermFilter.builder()
              .name(term)
              .type("selectable-concept")
              .values(List.of(TermFilterValue.builder().label("baz").count(values.length).build()))
              .build()
      );
    }
    // Hardcode availability filter. It is not yet decided if and how this will be available anyways. TODO!
    termFilters.add(TermFilter.builder()
        .type("boolean")
        .name("availability")
        .values(List.of())
        .build());
    return termFilters;
  }

  private SearchHits<OntologyListItemDocument> createDummySearchHitsPage(int totalHits) {
    var searchHitsList = new ArrayList<SearchHit<OntologyListItemDocument>>();

    for (int i = 0; i < totalHits; ++i) {
      searchHitsList.add(
          new SearchHit<>(
              null,
              null,
              null,
              10.0F,
              null,
              null,
              null,
              null,
              null,
              null,
              createDummyOntologyListItem(UUID.randomUUID().toString())
          )
      );
    }
    return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 10.0F, null, null, searchHitsList, null, null, null);
  }
}
