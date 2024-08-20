package de.numcodex.feasibility_gui_backend.terminology.es;

import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.model.Relative;
import de.numcodex.feasibility_gui_backend.terminology.es.model.TermFilter;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyListItemEsRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("terminology")
@Tag("elasticsearch")
@Import({TerminologyEsService.class})
@Testcontainers
@DataElasticsearchTest(        properties = {
    "app.elastic.filter=context,terminology"
})
public class TerminologyEsServiceIT {

  @Autowired
  private OntologyListItemEsRepository ontologyListItemEsRepository;

  @Autowired
  private OntologyItemEsRepository ontologyItemEsRepository;

  @Autowired
  private TerminologyEsService terminologyEsService;

  @Container
  public static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.14.1")
      .withEnv("discovery.type", "single-node")
      .withEnv("xpack.security.enabled", "false")
      .withExposedPorts(9200)
      .withStartupAttempts(3)
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500));

  @DynamicPropertySource
  static void esProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.elasticsearch.uris", elastic::getHttpHostAddress);
  }

  @BeforeAll
  static void setUp() throws IOException {
    elastic.start();
    System.out.println(elastic.getHttpHostAddress());
    WebClient webClient = WebClient.builder().baseUrl("http://" + elastic.getHttpHostAddress()).build();
    webClient.put()
        .uri("/ontology")
        .body(BodyInserters.fromResource(new ClassPathResource("ontology.json", TerminologyEsServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    webClient.post()
        .uri("/ontology/_bulk")
        .body(BodyInserters.fromResource(new ClassPathResource("ontology_testdata.json", TerminologyEsServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  @AfterAll
  static void tearDown() {
    elastic.stop();
  }

  @Test
  void testGetAvailableFilters() {
    List<TermFilter> availableFilters = terminologyEsService.getAvailableFilters();
    assertThat(availableFilters).isNotNull();

    assertThat(availableFilters)
        .hasSize(3)
        .extracting(TermFilter::name, TermFilter::type)
        .containsExactlyInAnyOrder(
            tuple("availability", "boolean"),
            tuple("context", "selectable-concept"),
            tuple("terminology", "selectable-concept")
        );
  }

  @Test
  void testPerformOntologySearchWithPaging_zeroResults() {
    var page = terminologyEsService.performOntologySearchWithPaging("random searchterm that is not found", null,null, null, null, false, 20, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isZero();
  }

  @Test
  void testPerformOntologySearchWithPaging_oneResult() {
    var page = terminologyEsService.performOntologySearchWithPaging("Hauttransplan", null, null, null, null, false, 20, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isOne();
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/ops");
    assertThat(page.results().get(0).termcode()).isEqualTo("5-925.gb");
  }

  @Test
  void testPerformOntologySearchWithPaging_multipleResults() {
    var page = terminologyEsService.performOntologySearchWithPaging("Blutdr", null, null, null, null, false, 20, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isEqualTo(3);
    assertThat(page.results().size()).isEqualTo(3);
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(page.results().get(0).termcode()).containsIgnoringCase("r03");
  }

  @Test
  void testPerformOntologySearchWithPaging_multipleResultsMultiplePagesPage0() {
    var page = terminologyEsService.performOntologySearchWithPaging("Blutdr", null, null, null, null, false, 2, 0);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isEqualTo(3);
    assertThat(page.results().size()).isEqualTo(2);
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(page.results().get(0).termcode()).containsIgnoringCase("r03");
  }

  @Test
  void testPerformOntologySearchWithPaging_multipleResultsMultiplePagesPage1() {
    var page = terminologyEsService.performOntologySearchWithPaging("Blutdr", null, null, null, null, false, 2, 1);
    assertThat(page).isNotNull();
    assertThat(page.totalHits()).isEqualTo(3);
    assertThat(page.results().size()).isEqualTo(1);
    assertThat(page.results().get(0).terminology()).isEqualTo("http://fhir.de/CodeSystem/bfarm/icd-10-gm");
    assertThat(page.results().get(0).termcode()).containsIgnoringCase("r03");
  }

  @Test
  void testGetSearchResultEntryByHash_succeeds() {
    String entryId = "e2fcb288-0d08-3272-8f32-64b8f1cfe095";
    EsSearchResultEntry entry = assertDoesNotThrow(() -> terminologyEsService.getSearchResultEntryByHash(entryId));
    assertThat(entry).isNotNull();
    assertThat(entry.id()).isEqualTo(entryId);
  }

  @Test
  void testGetSearchResultEntryByHash_throwsOnNotFound() {
    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getSearchResultEntryByHash("invalid-id"));
  }

  @Test
  void testGetSearchRelationsByHash_succeeds() {
    String entryId = "e2fcb288-0d08-3272-8f32-64b8f1cfe095";
    var relations = assertDoesNotThrow(() -> terminologyEsService.getOntologyItemRelationsByHash(entryId));
    assertThat(relations).isNotNull();
    assertThat(relations.parents()).isNotNull();
    assertThat(relations.parents()).isNotEmpty();
    assertThat(relations.parents().stream().toList().get(0)).isInstanceOf(Relative.class);
  }

  @Test
  void testGetSearchRelationsByHash_throwsOnNotFound() {
    assertThrows(OntologyItemNotFoundException.class, () -> terminologyEsService.getOntologyItemRelationsByHash("invalid-id"));
  }
}
