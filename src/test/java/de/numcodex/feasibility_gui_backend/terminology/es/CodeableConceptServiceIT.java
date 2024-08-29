package de.numcodex.feasibility_gui_backend.terminology.es;

import de.numcodex.feasibility_gui_backend.terminology.es.repository.CodeableConceptEsRepository;
import de.numcodex.feasibility_gui_backend.terminology.es.repository.OntologyItemNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("terminology")
@Tag("elasticsearch")
@Import({CodeableConceptService.class})
@Testcontainers
@DataElasticsearchTest(        properties = {
    "app.elastic.filter=context,terminology"
})
public class CodeableConceptServiceIT {

  @Autowired
  private ElasticsearchOperations operations;

  @Autowired
  private CodeableConceptEsRepository repo;

  @Autowired
  private CodeableConceptService codeableConceptService;

  @Container
  @ServiceConnection
  public static ElasticsearchContainer ELASTICSEARCH_CONTAINER = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.16.1")
      .withEnv("discovery.type", "single-node")
      .withEnv("xpack.security.enabled", "false")
      .withReuse(false)
      .withExposedPorts(9200)
      .withStartupAttempts(3)
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500));

  @BeforeAll
  static void setUp() throws InterruptedException {
    ELASTICSEARCH_CONTAINER.start();
    WebClient webClient = WebClient.builder().baseUrl("http://" + ELASTICSEARCH_CONTAINER.getHttpHostAddress()).build();
    webClient.put()
        .uri("/codeable_concept")
        .body(BodyInserters.fromResource(new ClassPathResource("codeable_concept.json", CodeableConceptServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    webClient.post()
        .uri("/codeable_concept/_bulk")
        .body(BodyInserters.fromResource(new ClassPathResource("cc_testdata.json", CodeableConceptServiceIT.class)))
        .retrieve()
        .toBodilessEntity()
        .block();

    // When running in github actions without a slight delay, the data might not be complete in the elastic search container (although a blocking call is used)
    Thread.sleep(1000);
  }

  @AfterAll
  static void tearDown() {
    ELASTICSEARCH_CONTAINER.stop();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsOne() {
    var page = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("foo", List.of(), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isOne();
    Assertions.assertEquals("A1.0", page.getResults().get(0).termCode().code());
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsNone() {
    var page = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("something-not-found", List.of(), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isZero();
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsAllWithNoKeyword() {
    var page = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("", List.of(), 20, 0));

    assertNotNull(page);
    assertThat(page.getTotalHits()).isEqualTo(3L);
  }

  @Test
  void testPerformCodeableConceptSearchWithRepoAndPaging_findsTwoOrOneDependingOnFilter() {
    var pageNoFilter = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("ba", List.of(), 20, 0));
    var pageOneFilter = assertDoesNotThrow (() -> codeableConceptService.performCodeableConceptSearchWithRepoAndPaging("ba", List.of("some-value-set"), 20, 0));

    assertNotNull(pageNoFilter);
    assertNotNull(pageOneFilter);
    Assertions.assertEquals(2, pageNoFilter.getTotalHits());
    Assertions.assertEquals(1, pageOneFilter.getTotalHits());
  }

  @Test
  void testGetSearchResultEntryByCode_succeeds() {
    var result = assertDoesNotThrow(() -> codeableConceptService.getSearchResultEntryByCode("A1.1"));

    assertNotNull(result);
    Assertions.assertEquals("bar", result.termCode().display());
    Assertions.assertEquals("A1.1", result.termCode().code());
    Assertions.assertEquals("2012", result.termCode().version());
    Assertions.assertEquals("another-system", result.termCode().system());
  }

  @Test
  void testGetSearchResultEntryByCode_throwsOnNotFound() {
    assertThrows(OntologyItemNotFoundException.class, () -> codeableConceptService.getSearchResultEntryByCode("something-not-found"));
  }
}
