package de.numcodex.feasibility_gui_backend.terminology.v3;

import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingInterceptor;
import de.numcodex.feasibility_gui_backend.query.ratelimiting.RateLimitingServiceSpringConfig;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResult;
import de.numcodex.feasibility_gui_backend.terminology.api.EsSearchResultEntry;
import de.numcodex.feasibility_gui_backend.terminology.es.TerminologyEsService;
import de.numcodex.feasibility_gui_backend.terminology.es.config.Config;
import de.numcodex.feasibility_gui_backend.terminology.es.model.OntologyItemRelationsDocument;
import de.numcodex.feasibility_gui_backend.terminology.es.model.Relative;
import de.numcodex.feasibility_gui_backend.terminology.es.model.Translation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Tag("terminology")
@ExtendWith(SpringExtension.class)
@Import(RateLimitingServiceSpringConfig.class)
@WebMvcTest(
    controllers = TerminologyEsRestController.class
)
@Testcontainers
//@ContextConfiguration(classes = Config.class)
class TerminologyEsRestControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TerminologyEsService terminologyEsService;

  @MockBean
  private RateLimitingInterceptor rateLimitingInterceptor;

//  @Container
//  public static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.14.1")
//      .withEnv("discovery.type", "single-node")
//      .withEnv("xpack.security.enabled", "false")
//      .withExposedPorts(9200)
//      .withStartupAttempts(3)
//      .withImagePullPolicy(PullPolicy.alwaysPull())
//      .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500));
//
//  @BeforeAll
//  static void setUp() throws IOException {
//    elastic.start();
//    System.out.println(elastic.getHttpHostAddress());
//    WebClient webClient = WebClient.builder().baseUrl("http://" + elastic.getHttpHostAddress()).build();
//    webClient.put()
//        .uri("/ontology")
//        .body(BodyInserters.fromResource(new ClassPathResource("ontology.json", TerminologyEsRestControllerIT.class)))
//        .retrieve()
//        .toBodilessEntity()
//        .block();
//
//    webClient.post()
//        .uri("/ontology/_bulk")
//        .body(BodyInserters.fromResource(new ClassPathResource("testData.json", TerminologyEsRestControllerIT.class)))
//        .retrieve()
//        .toBodilessEntity()
//        .block();
//  }
//
//  @AfterAll
//  static void tearDown() {
//    elastic.stop();
//  }

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testGetFilters_succeeds() throws Exception {
    List<String> filterList = List.of("context", "kdsModule", "terminology");
    doReturn(filterList).when(terminologyEsService).getAvailableFilters();

    mockMvc.perform(get(URI.create("/api/v3/terminology/search/filter")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(filterList.stream()
            .map(s -> "\"" + s + "\"")
            .collect(Collectors.joining(",", "[", "]"))));
  }

  @Test
  public void testGetFilters_failsOnUnauthorized() throws Exception {
    List<String> filterList = List.of("context", "kdsModule", "terminology");
    doReturn(filterList).when(terminologyEsService).getAvailableFilters();

    mockMvc.perform(get(URI.create("/api/v3/terminology/search/filter")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testSearchOntologyItemsCriteriaQuery_succeeds() throws Exception {
    var totalHits = 1;
    var dummyEsSearchResult = createDummyEsSearchResult(totalHits);
    doReturn(dummyEsSearchResult).when(terminologyEsService).performOntologySearchWithRepoAndPaging(any(String.class), isNull(), isNull(), isNull(), anyBoolean(), anyInt(), anyInt());

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/search")).param("searchterm", "some-context").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHits").value(dummyEsSearchResult.getTotalHits()))
        .andExpect(jsonPath("$.results[0].id").value(dummyEsSearchResult.getResults().get(0).getId()))
        .andExpect(jsonPath("$.results[0].name").value(dummyEsSearchResult.getResults().get(0).getName()))
        .andExpect(jsonPath("$.results[0].terminology").value(dummyEsSearchResult.getResults().get(0).getTerminology()))
        .andExpect(jsonPath("$.results[0].selectable").value(dummyEsSearchResult.getResults().get(0).isSelectable()))
        .andExpect(jsonPath("$.results[0].kdsModule").value(dummyEsSearchResult.getResults().get(0).getKdsModule()))
        .andExpect(jsonPath("$.results[0].availability").value(dummyEsSearchResult.getResults().get(0).getAvailability()))
        .andExpect(jsonPath("$.results[0].context").value(dummyEsSearchResult.getResults().get(0).getContext()));

  }

  @Test
  public void testSearchOntologyItemsCriteriaQuery_failsOnUnauthorized() throws Exception {
    doReturn(createDummyEsSearchResult(1)).when(terminologyEsService).performOntologySearchWithRepoAndPaging(any(String.class), anyList(), anyList(), anyList(), any(Boolean.class), any(Integer.class), any(Integer.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/search")).param("searchterm", "some-context").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testGetOntologyItemRelationsByHash_succeeds() throws Exception {
    var dummyOntologyItemRelations = createDummyOntologyItemRelations();
    doReturn(dummyOntologyItemRelations).when(terminologyEsService).getOntologyItemRelationsByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc/relations")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.translations[0].lang").value(dummyOntologyItemRelations.getTranslations().stream().toList().get(0).getLang()))
        .andExpect(jsonPath("$.translations[0].value").value(dummyOntologyItemRelations.getTranslations().stream().toList().get(0).getValue()))
        .andExpect(jsonPath("$.children[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.getChildren().stream().toList().get(0).getContextualizedTermcodeHash()))
        .andExpect(jsonPath("$.children[0].name").value(dummyOntologyItemRelations.getChildren().stream().toList().get(0).getName()))
        .andExpect(jsonPath("$.parents[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.getParents().stream().toList().get(0).getContextualizedTermcodeHash()))
        .andExpect(jsonPath("$.parents[0].name").value(dummyOntologyItemRelations.getParents().stream().toList().get(0).getName()))
        .andExpect(jsonPath("$.relatedTerms[0].contextualizedTermcodeHash").value(dummyOntologyItemRelations.getRelatedTerms().stream().toList().get(0).getContextualizedTermcodeHash()))
        .andExpect(jsonPath("$.relatedTerms[0].name").value(dummyOntologyItemRelations.getRelatedTerms().stream().toList().get(0).getName()));
  }

  @Test
  public void testGetOntologyItemRelationsByHash_failsOnUnauthorized() throws Exception {
    var dummyOntologyItemRelations = createDummyOntologyItemRelations();
    doReturn(dummyOntologyItemRelations).when(terminologyEsService).getOntologyItemRelationsByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc/relations")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "FEASIBILITY_TEST_USER")
  public void testGetOntologyItemByHash_succeeds() throws Exception {
    var dummySearchResultEntry = createDummyEsSearchResultEntry();
    doReturn(dummySearchResultEntry).when(terminologyEsService).getSearchResultEntryByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(dummySearchResultEntry.getId()))
        .andExpect(jsonPath("$.name").value(dummySearchResultEntry.getName()))
        .andExpect(jsonPath("$.availability").value(dummySearchResultEntry.getAvailability()))
        .andExpect(jsonPath("$.context").value(dummySearchResultEntry.getContext()))
        .andExpect(jsonPath("$.terminology").value(dummySearchResultEntry.getTerminology()))
        .andExpect(jsonPath("$.termcode").value(dummySearchResultEntry.getTermcode()))
        .andExpect(jsonPath("$.kdsModule").value(dummySearchResultEntry.getKdsModule()))
        .andExpect(jsonPath("$.selectable").value(dummySearchResultEntry.isSelectable()));
  }

  @Test
  public void testGetOntologyItemByHash_failsOnUnauthorized() throws Exception {
    var dummySearchResultEntry = createDummyEsSearchResultEntry();
    doReturn(dummySearchResultEntry).when(terminologyEsService).getSearchResultEntryByHash(any(String.class));

    mockMvc.perform(get(URI.create("/api/v3/terminology/entry/abc")).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  private EsSearchResult createDummyEsSearchResult(int totalHits) {
    return EsSearchResult.builder()
        .totalHits(totalHits)
        .results(List.of(createDummyEsSearchResultEntry()))
        .build();
  }

  private EsSearchResultEntry createDummyEsSearchResultEntry() {
    return EsSearchResultEntry.builder()
        .terminology("some-terminology")
        .availability(100)
        .context("some-context")
        .id("abc-123")
        .kdsModule("some-module")
        .name("some-name")
        .selectable(true)
        .build();
  }

  private OntologyItemRelationsDocument createDummyOntologyItemRelations() {
    return OntologyItemRelationsDocument.builder()
        .relatedTerms(List.of(createDummyRelative()))
        .translations(List.of(createDummyTranslation()))
        .parents(List.of(createDummyRelative()))
        .children(List.of(createDummyRelative()))
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
}
